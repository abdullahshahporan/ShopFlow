package com.shahporan.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/products", "/access-denied", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products").hasRole("SELLER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasRole("SELLER")
                        .requestMatchers(HttpMethod.POST, "/api/orders").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/my").hasRole("BUYER")
                        .requestMatchers(HttpMethod.GET, "/api/orders/*").hasAnyRole("BUYER", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/buyer/**").hasRole("BUYER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
