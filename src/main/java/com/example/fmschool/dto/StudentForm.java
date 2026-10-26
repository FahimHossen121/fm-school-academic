package com.example.fmschool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentForm {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must be at most 255 characters")
    @Pattern(
            regexp = "[a-zA-Z ]+|student[0-9]+",
            message = "Use letters/spaces or a demo label such as student2"
    )
    private String name;

    @NotBlank(message = "Roll is required")
    @Size(max = 255, message = "Roll must be at most 255 characters")
    @Pattern(
            regexp = "[0-9]+",
            message = "Roll must contain digits only"
    )
    private String roll;
}
