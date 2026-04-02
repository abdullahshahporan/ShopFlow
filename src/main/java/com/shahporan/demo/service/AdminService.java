package com.shahporan.demo.service;

import com.shahporan.demo.dto.UserResponseDto;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.AdminRepository;
import com.shahporan.demo.repository.SellerRepository;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.RoleMappings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    public List<UserResponseDto> getAllUsers() {
        List<UserResponseDto> buyers = userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> UserResponseDto.builder()
                        .id(u.getId()).name(u.getName()).email(u.getEmail())
                        .roleInt(RoleMappings.BUYER).role("ROLE_BUYER")
                        .enabled(u.getEnabled()).createdAt(u.getCreatedAt()).build()).toList();
        List<UserResponseDto> sellers = sellerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> UserResponseDto.builder()
                        .id(s.getId()).name(s.getName()).email(s.getEmail())
                        .roleInt(RoleMappings.SELLER).role("ROLE_SELLER")
                        .enabled(s.getEnabled()).createdAt(s.getCreatedAt()).build()).toList();
        List<UserResponseDto> admins = adminRepository.findAll().stream()
                .map(a -> UserResponseDto.builder()
                        .id(a.getId()).name(a.getName()).email(a.getEmail())
                        .roleInt(RoleMappings.ADMIN).role("ROLE_ADMIN")
                        .enabled(a.getEnabled()).createdAt(a.getCreatedAt()).build()).toList();
        return Stream.of(buyers, sellers, admins).flatMap(List::stream)
                .sorted(Comparator.comparing(UserResponseDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<UserResponseDto> getAllSellers() {
        return sellerRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(s -> UserResponseDto.builder()
                        .id(s.getId()).name(s.getName()).email(s.getEmail())
                        .roleInt(RoleMappings.SELLER).role("ROLE_SELLER")
                        .enabled(s.getEnabled()).createdAt(s.getCreatedAt()).build()).toList();
    }

    public long countPendingSellers() {
        return sellerRepository.countByEnabled(false);
    }

    public long countBuyers() {
        return userRepository.count();
    }

    public long countSellers() {
        return sellerRepository.count();
    }

    public long countAdmins() {
        return adminRepository.count();
    }

    @Transactional
    public UserResponseDto toggleUser(Long userId, Long currentAdminId) {
        var seller = sellerRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + userId));

        seller.setEnabled(!Boolean.TRUE.equals(seller.getEnabled()));
        seller = sellerRepository.save(seller);

        return UserResponseDto.builder()
                .id(seller.getId())
                .name(seller.getName())
                .email(seller.getEmail())
                .roleInt(RoleMappings.SELLER)
                .role("ROLE_SELLER")
                .enabled(seller.getEnabled())
                .createdAt(seller.getCreatedAt())
                .build();
    }
}
