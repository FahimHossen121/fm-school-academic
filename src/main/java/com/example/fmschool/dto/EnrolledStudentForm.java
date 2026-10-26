package com.example.fmschool.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class EnrolledStudentForm {
    @NotBlank(message = "Name is required")
    @Size(max = 255)
    @Pattern(regexp = "[a-zA-Z ]+|student[0-9]+", message = "Use letters/spaces or the matching student2/teacher2 demo label")
    private String name;

    @NotBlank(message = "Roll is required")
    @Size(max = 255)
    @Pattern(regexp = "[0-9]+", message = "Roll must contain digits only")
    private String roll;

    @DecimalMin(value = "0", message = "Marks cannot be below 0")
    @DecimalMax(value = "100", message = "Marks cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Use at most two decimal places")
    private BigDecimal marks;
}
