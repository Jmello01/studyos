package com.studyos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AreaRequest(
        @NotBlank(message = "é obrigatório") @Size(max = 100, message = "deve ter no máximo 100 caracteres") String name,
        @Size(max = 1000, message = "deve ter no máximo 1000 caracteres") String description,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "deve estar no formato #RRGGBB") String color,
        @Size(max = 50, message = "deve ter no máximo 50 caracteres") String icon,
        @Min(value = 0, message = "não pode ser negativa") Integer position) {

    public AreaRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}