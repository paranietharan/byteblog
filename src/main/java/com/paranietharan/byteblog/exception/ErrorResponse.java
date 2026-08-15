package com.paranietharan.byteblog.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    @Schema(example = "2026-08-15T10:00:00")
    private LocalDateTime timestamp;
    @Schema(example = "400")
    private int status;
    @Schema(example = "Validation Error")
    private String error;
    @Schema(example = "password: Password must be between 12 and 100 characters")
    private String message;
    @Schema(example = "/api/v1/auth/register")
    private String path;
}
