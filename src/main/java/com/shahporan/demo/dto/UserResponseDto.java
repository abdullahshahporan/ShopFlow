package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserResponseDto {
    Long id;
    String name;
    String email;
    Integer roleInt;
    String role;
    Boolean enabled;
    LocalDateTime createdAt;
}
