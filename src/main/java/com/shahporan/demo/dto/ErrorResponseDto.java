package com.shahporan.demo.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ErrorResponseDto {
    LocalDateTime timestamp;
    int status;
    String message;
    String path;
}
