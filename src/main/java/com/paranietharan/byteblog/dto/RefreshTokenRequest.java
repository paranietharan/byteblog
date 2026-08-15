package com.paranietharan.byteblog.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(example = "M4qQeL-x5jK8s9n2dF0vP7aB3cR6tY1uW8zN5hG2kLs")
    private String refreshToken;
}
