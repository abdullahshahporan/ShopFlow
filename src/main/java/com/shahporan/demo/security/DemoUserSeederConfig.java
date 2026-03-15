package com.shahporan.demo.security;

import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoUserSeederConfig {

    @Bean
    public CommandLineRunner seedDemoUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            createIfMissing(userRepository, passwordEncoder,
                    "Admin User", "admin@demo.com", "Admin123!", RoleMappings.ADMIN);
            createIfMissing(userRepository, passwordEncoder,
                    "Seller User", "seller@demo.com", "Seller123!", RoleMappings.SELLER);
            createIfMissing(userRepository, passwordEncoder,
                    "Buyer User", "buyer@demo.com", "Buyer123!", RoleMappings.BUYER);
        };
    }

    private void createIfMissing(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder,
                                 String name,
                                 String email,
                                 String rawPassword,
                                 int roleInt) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .roleInt(roleInt)
                .enabled(true)
                .build();

        userRepository.save(user);
    }
}

