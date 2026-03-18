package com.shahporan.demo.controller;

import com.shahporan.demo.dto.OrderRequestDto;
import com.shahporan.demo.dto.OrderResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderRestController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDto> create(@Valid @RequestBody OrderRequestDto dto,
                                                   Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(dto, user.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrdersByBuyer(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getById(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        String role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).findFirst().orElse("ROLE_BUYER");
        return ResponseEntity.status(HttpStatus.OK).body(orderService.getOrderById(id, user.getId(), role));
    }
}
