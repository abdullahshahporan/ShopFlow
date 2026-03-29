package com.shahporan.demo.service;

import com.shahporan.demo.dto.UserResponseDto;
import com.shahporan.demo.exception.BadRequestException;
import com.shahporan.demo.exception.ResourceNotFoundException;
import com.shahporan.demo.repository.UserRepository;
import com.shahporan.demo.security.RoleMappings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc().stream().map(user -> UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roleInt(user.getRoleInt())
                .role(RoleMappings.toAuthority(user.getRoleInt()))
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build()).toList();
    }

    public long countPendingSellers() {
        return userRepository.countByRoleIntAndEnabled(RoleMappings.SELLER, false);
    }

    @Transactional
    public UserResponseDto changeRole(Long userId, Integer roleInt) {
        if (roleInt == null || roleInt < 0 || roleInt > 2) {
            throw new BadRequestException("roleInt must be 0, 1 or 2");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        user.setRoleInt(roleInt);
        user = userRepository.save(user);

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roleInt(user.getRoleInt())
                .role(RoleMappings.toAuthority(user.getRoleInt()))
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserResponseDto toggleUser(Long userId, Long currentAdminId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getId().equals(currentAdminId)) {
            throw new BadRequestException("Admin cannot disable their own account");
        }

        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        user = userRepository.save(user);

        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .roleInt(user.getRoleInt())
                .role(RoleMappings.toAuthority(user.getRoleInt()))
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
