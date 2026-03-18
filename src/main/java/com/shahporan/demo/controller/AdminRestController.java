package com.shahporan.demo.controller;

import com.shahporan.demo.dto.UpdateRoleRequestDto;
import com.shahporan.demo.dto.UserResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> users() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllUsers());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<UserResponseDto> changeRole(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateRoleRequestDto dto) {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.changeRole(id, dto.getRoleInt()));
    }

    @PutMapping("/users/{id}/toggle")
    public ResponseEntity<UserResponseDto> toggle(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails current = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(adminService.toggleUser(id, current.getId()));
    }
}
