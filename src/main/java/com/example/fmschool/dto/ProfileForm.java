package com.example.fmschool.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    @Pattern(regexp = "[a-zA-Z ]+|student[0-9]+|teacher[0-9]+", message = "Use letters/spaces or the matching student2/teacher2 demo label")
    private String name;

    @Size(max = 255)
    @Pattern(regexp = "[0-9]*", message = "Roll must contain digits only")
    private String roll;

    @Size(max = 255)
    private String course;

}
