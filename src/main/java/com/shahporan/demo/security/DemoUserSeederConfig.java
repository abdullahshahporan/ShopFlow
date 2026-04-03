package com.shahporan.demo.security;

import com.shahporan.demo.entity.Admin;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserSeederConfig {

    @Bean
        public CommandLineRunner seedDemoUsers(AdminRepository adminRepository,
                           SellerRepository sellerRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        return args -> {
            createAdminIfMissing(adminRepository, passwordEncoder,
                "Admin User", "admin@demo.com", "Admin123!");
            createSellerIfMissing(sellerRepository, passwordEncoder,
                "Seller User", "seller@demo.com", "Seller123!");
            createBuyerIfMissing(userRepository, passwordEncoder,
                "Buyer User", "buyer@demo.com", "Buyer123!");
        };
    }

        private void createAdminIfMissing(AdminRepository adminRepository,
                          PasswordEncoder passwordEncoder,
                          String name,
                          String email,
                          String rawPassword) {
        if (adminRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Admin admin = Admin.builder()
            .name(name)
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .enabled(true)
            .build();

        adminRepository.save(admin);
        }

        private void createSellerIfMissing(SellerRepository sellerRepository,
                           PasswordEncoder passwordEncoder,
                           String name,
                           String email,
                           String rawPassword) {
        if (sellerRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        Seller seller = Seller.builder()
            .name(name)
            .email(email)
            .passwordHash(passwordEncoder.encode(rawPassword))
            .enabled(true)
            .build();

        sellerRepository.save(seller);
        }

        private void createBuyerIfMissing(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          String name,
                          String email,
                          String rawPassword) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
            .roleInt(RoleMappings.BUYER)
                .enabled(true)
                .build();

        userRepository.save(user);
    }
}

