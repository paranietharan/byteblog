package com.paranietharan.byteblog.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private String message;
    private Boolean success;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public MessageResponse(String message, Boolean success) {
        this.message = message;
        this.success = success;
        this.timestamp = LocalDateTime.now();
    }
}
