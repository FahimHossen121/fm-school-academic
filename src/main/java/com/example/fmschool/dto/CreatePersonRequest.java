package com.example.fmschool.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CreatePersonRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "[a-zA-Z0-9._-]+") String username,
        @NotBlank @Size(min = 8, max = 64) String password,
        @NotNull @Valid ProfileForm profile) {
}
