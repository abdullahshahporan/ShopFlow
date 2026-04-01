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

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    public List<UserResponseDto> getAllUsers() {
        return sellerRepository.findAllByOrderByCreatedAtDesc().stream().map(seller -> UserResponseDto.builder()
                .id(seller.getId())
                .name(seller.getName())
                .email(seller.getEmail())
                .roleInt(RoleMappings.SELLER)
                .role("ROLE_SELLER")
                .enabled(seller.getEnabled())
                .createdAt(seller.getCreatedAt())
                .build()).toList();
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
