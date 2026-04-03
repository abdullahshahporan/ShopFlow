package com.shahporan.demo.service;

import com.shahporan.demo.entity.Admin;
import com.shahporan.demo.entity.Seller;
import com.shahporan.demo.entity.User;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.security.RoleMappings;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    public CustomUserDetailsService(AdminRepository adminRepository,
                                    SellerRepository sellerRepository,
                                    UserRepository userRepository) {
        this.adminRepository = adminRepository;
        this.sellerRepository = sellerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim();

        Admin admin = adminRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (admin != null) {
            return buildUserDetails(admin.getId(), admin.getEmail(), admin.getPasswordHash(), RoleMappings.ADMIN, admin.getEnabled());
        }

        Seller seller = sellerRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (seller != null) {
            return buildUserDetails(seller.getId(), seller.getEmail(), seller.getPasswordHash(), RoleMappings.SELLER, seller.getEnabled());
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return buildUserDetails(user.getId(), user.getEmail(), user.getPasswordHash(), RoleMappings.BUYER, user.getEnabled());
    }

    private UserDetails buildUserDetails(Long id,
                                         String email,
                                         String passwordHash,
                                         Integer roleInt,
                                         Boolean enabled) {
        return CustomUserDetails.builder()
            .id(id)
            .username(email)
            .password(passwordHash)
            .authorities(java.util.List.of(new SimpleGrantedAuthority(RoleMappings.toAuthority(roleInt))))
            .enabled(Boolean.TRUE.equals(enabled))
                .build();
    }
}

