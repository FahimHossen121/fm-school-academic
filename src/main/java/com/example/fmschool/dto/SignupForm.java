package com.example.fmschool.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupForm {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
    @Pattern(regexp = "[a-zA-Z0-9._-]+",
            message = "Username may contain letters, digits, periods, underscores or hyphens")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be 8–64 characters")
    private String password;

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    @Pattern(regexp = "[a-zA-Z ]+|student[0-9]+|teacher[0-9]+",
            message = "Use letters/spaces or a demo label such as student2")
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "[0-9]*", message = "Roll must contain digits only")
    private String roll;

    @Size(max = 255)
    private String course;
}
