package com.shahporan.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRoleRequestDto {

    @NotNull(message = "roleInt is required")
    @Min(value = 0, message = "roleInt must be 0, 1 or 2")
    @Max(value = 2, message = "roleInt must be 0, 1 or 2")
    private Integer roleInt;
}
