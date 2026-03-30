package com.shahporan.demo.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class AccountTableMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
  private final DataSource dataSource;

    @Override
    public void run(String... args) {
    if (!isPostgreSql()) {
      return;
    }

        // Ensure newly introduced order payment columns exist in older databases.
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20)");
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20)");
        jdbcTemplate.update("UPDATE orders SET payment_method = 'COD' WHERE payment_method IS NULL");
        jdbcTemplate.update("UPDATE orders SET payment_status = CASE WHEN status = 'CONFIRMED' THEN 'PAID' ELSE 'PENDING' END WHERE payment_status IS NULL");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_method SET DEFAULT 'COD'");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_status SET DEFAULT 'PENDING'");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_method SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL");

        // Move existing SELLER and ADMIN rows out of users table into dedicated tables.
        jdbcTemplate.update("""
                INSERT INTO sellers(name, email, password_hash, enabled, created_at)
                SELECT u.name, u.email, u.password_hash, u.enabled, u.created_at
                FROM users u
                WHERE u.role_int = 1
                  AND NOT EXISTS (
                      SELECT 1 FROM sellers s WHERE lower(s.email) = lower(u.email)
                  )
                """);

        jdbcTemplate.update("""
                INSERT INTO admins(name, email, password_hash, enabled, created_at)
                SELECT u.name, u.email, u.password_hash, u.enabled, u.created_at
                FROM users u
                WHERE u.role_int = 2
                  AND NOT EXISTS (
                      SELECT 1 FROM admins a WHERE lower(a.email) = lower(u.email)
                  )
                """);

        // Repoint product and stock references from legacy seller rows in users to new sellers rows.
        jdbcTemplate.update("""
                UPDATE products p
                SET seller_id = s.id
                FROM users u
                JOIN sellers s ON lower(s.email) = lower(u.email)
                WHERE u.role_int = 1
                  AND p.seller_id = u.id
                """);

        jdbcTemplate.update("""
                UPDATE stock_movements sm
                SET seller_id = s.id
                FROM users u
                JOIN sellers s ON lower(s.email) = lower(u.email)
                WHERE u.role_int = 1
                  AND sm.seller_id = u.id
                """);

            // Replace legacy foreign keys (seller_id -> users.id) with seller_id -> sellers.id.
            jdbcTemplate.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                  FOR r IN
                    SELECT c.conname
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = t.relnamespace
                    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                    WHERE c.contype = 'f'
                      AND n.nspname = 'public'
                      AND t.relname = 'products'
                      AND a.attname = 'seller_id'
                  LOOP
                    EXECUTE format('ALTER TABLE products DROP CONSTRAINT %I', r.conname);
                  END LOOP;
                END $$;
                """);

            jdbcTemplate.execute("""
                ALTER TABLE products
                ADD CONSTRAINT fk_products_seller
                FOREIGN KEY (seller_id) REFERENCES sellers(id)
                """);

            jdbcTemplate.execute("""
                DO $$
                DECLARE r record;
                BEGIN
                  FOR r IN
                    SELECT c.conname
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = t.relnamespace
                    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                    WHERE c.contype = 'f'
                      AND n.nspname = 'public'
                      AND t.relname = 'stock_movements'
                      AND a.attname = 'seller_id'
                  LOOP
                    EXECUTE format('ALTER TABLE stock_movements DROP CONSTRAINT %I', r.conname);
                  END LOOP;
                END $$;
                """);

            jdbcTemplate.execute("""
                ALTER TABLE stock_movements
                ADD CONSTRAINT fk_stock_movements_seller
                FOREIGN KEY (seller_id) REFERENCES sellers(id)
                """);

        jdbcTemplate.update("DELETE FROM users WHERE role_int IN (1, 2)");
    }

  private boolean isPostgreSql() {
    try (Connection connection = dataSource.getConnection()) {
      String dbName = connection.getMetaData().getDatabaseProductName();
      return dbName != null && dbName.toLowerCase().contains("postgresql");
    } catch (SQLException ex) {
      return false;
    }
  }
}
