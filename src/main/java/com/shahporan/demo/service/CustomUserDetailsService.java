package com.shahporan.demo.service;

import com.shahporan.demo.entity.User;
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

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return CustomUserDetails.builder()
            .id(user.getId())
            .username(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(java.util.List.of(new SimpleGrantedAuthority(RoleMappings.toAuthority(user.getRoleInt()))))
            .enabled(Boolean.TRUE.equals(user.getEnabled()))
                .build();
    }
}

