package com.paranietharan.byteblog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Schema(example = "parani@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Schema(example = "correct-horse-battery-staple")
    private String password;
}
