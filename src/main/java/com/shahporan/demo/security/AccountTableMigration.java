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
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_id BIGINT");
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS qty INTEGER");
        jdbcTemplate.execute("ALTER TABLE orders ADD COLUMN IF NOT EXISTS unit_price NUMERIC(12, 2)");
        jdbcTemplate.update("UPDATE orders SET payment_method = 'COD' WHERE payment_method IS NULL");
        jdbcTemplate.update("UPDATE orders SET payment_status = CASE WHEN status = 'CONFIRMED' THEN 'PAID' ELSE 'PENDING' END WHERE payment_status IS NULL");

        // Backfill flattened order columns from order_items when legacy table exists.
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema = 'public' AND table_name = 'order_items'
                  ) THEN
                    UPDATE orders o
                    SET product_id = oi.product_id,
                        qty = oi.qty,
                        unit_price = oi.unit_price,
                        total = COALESCE(oi.unit_price, 0) * COALESCE(oi.qty, 0)
                    FROM (
                        SELECT DISTINCT ON (order_id)
                               order_id, product_id, qty, unit_price
                        FROM order_items
                        ORDER BY order_id, id
                    ) oi
                    WHERE o.id = oi.order_id
                      AND (o.product_id IS NULL OR o.qty IS NULL OR o.unit_price IS NULL);
                  END IF;
                END $$;
                """);

        jdbcTemplate.update("UPDATE orders SET qty = 1 WHERE qty IS NULL OR qty < 1");
        jdbcTemplate.update("UPDATE orders SET unit_price = total WHERE unit_price IS NULL");
        jdbcTemplate.update("UPDATE orders SET total = unit_price * qty WHERE total IS NULL");
        jdbcTemplate.update("DELETE FROM orders WHERE product_id IS NULL");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_method SET DEFAULT 'COD'");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_status SET DEFAULT 'PENDING'");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_method SET NOT NULL");
        jdbcTemplate.execute("ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL");

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_product'
                  ) THEN
                    ALTER TABLE orders
                    ADD CONSTRAINT fk_orders_product
                    FOREIGN KEY (product_id) REFERENCES products(id);
                  END IF;
                END $$;
                """);

        // Normalize old status naming.
        jdbcTemplate.update("UPDATE orders SET status = 'APPROVED' WHERE upper(status) = 'CONFIRMED'");

        // Ensure stock table exists and backfill from products table.
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS stock (
                    id BIGSERIAL PRIMARY KEY,
                    product_id BIGINT NOT NULL UNIQUE,
                    seller_id BIGINT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP
                )
                """);

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_stock_product'
                  ) THEN
                    ALTER TABLE stock
                    ADD CONSTRAINT fk_stock_product
                    FOREIGN KEY (product_id) REFERENCES products(id);
                  END IF;
                END $$;
                """);

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_stock_seller'
                  ) THEN
                    ALTER TABLE stock
                    ADD CONSTRAINT fk_stock_seller
                    FOREIGN KEY (seller_id) REFERENCES sellers(id);
                  END IF;
                END $$;
                """);

        jdbcTemplate.update("""
                INSERT INTO stock(product_id, seller_id, quantity, created_at, updated_at)
                SELECT p.id, p.seller_id, COALESCE(p.quantity, 0), NOW(), NOW()
                FROM products p
                WHERE NOT EXISTS (
                    SELECT 1 FROM stock s WHERE s.product_id = p.id
                )
                """);

        // Ensure cancel_order table exists and move legacy cancelled rows from orders table.
        jdbcTemplate.execute("""
          CREATE TABLE IF NOT EXISTS cancel_order (
                    id BIGSERIAL PRIMARY KEY,
                    original_order_id BIGINT NOT NULL UNIQUE,
                    buyer_id BIGINT NOT NULL,
                    status VARCHAR(30) NOT NULL DEFAULT 'CANCELLED',
                    reason VARCHAR(500),
                    total NUMERIC(14, 2) NOT NULL DEFAULT 0,
                    payment_method VARCHAR(20) NOT NULL DEFAULT 'COD',
                    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    items_snapshot TEXT,
                    order_created_at TIMESTAMP,
                    cancelled_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """);

        jdbcTemplate.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_cancel_order_buyer'
                  ) THEN
                    ALTER TABLE cancel_order
                    ADD CONSTRAINT fk_cancel_order_buyer
                    FOREIGN KEY (buyer_id) REFERENCES users(id);
                  END IF;
                END $$;
                """);

        jdbcTemplate.update("""
                INSERT INTO cancel_order(
                    original_order_id,
                    buyer_id,
                    status,
                    reason,
                    total,
                    payment_method,
                    payment_status,
                    order_created_at,
                    cancelled_at
                )
                SELECT
                    o.id,
                    o.buyer_id,
                    'CANCELLED',
                    'Migrated from orders table',
                    o.total,
                    COALESCE(o.payment_method, 'COD'),
                    COALESCE(o.payment_status, 'PENDING'),
                    o.created_at,
                    NOW()
                FROM orders o
                WHERE upper(o.status) = 'CANCELLED'
                  AND NOT EXISTS (
                    SELECT 1 FROM cancel_order c WHERE c.original_order_id = o.id
                  )
                """);

        jdbcTemplate.update("DELETE FROM orders WHERE upper(status) = 'CANCELLED'");

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
                DROP TABLE IF EXISTS order_items CASCADE
                """);

            jdbcTemplate.execute("""
                DROP TABLE IF EXISTS stock_movements CASCADE
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
