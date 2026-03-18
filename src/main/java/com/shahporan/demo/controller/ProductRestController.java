package com.shahporan.demo.controller;

import com.shahporan.demo.dto.ProductRequestDto;
import com.shahporan.demo.dto.ProductResponseDto;
import com.shahporan.demo.security.CustomUserDetails;
import com.shahporan.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAll() {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getAllActiveProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(productService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> create(@Valid @RequestBody ProductRequestDto dto,
                                                     Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(dto, user.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody ProductRequestDto dto,
                                                     Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(id, dto, user.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        productService.deleteProduct(id, user.getId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
