package com.example.fmschool.dto;

import com.example.fmschool.entity.Role;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountForm {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "[a-zA-Z0-9._-]+",
            message = "Use letters, digits, periods, underscores or hyphens")
    private String username;

    @NotBlank
    @Size(min = 8, max = 64, message = "Password must be 8 to 64 characters")
    private String password;

    @NotNull(message = "Choose a role")
    private Role role;
}
