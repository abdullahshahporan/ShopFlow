package com.shahporan.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {

    @NotEmpty(message = "At least one order item is required")
    @Valid
    private List<OrderItemRequestDto> items;
}
