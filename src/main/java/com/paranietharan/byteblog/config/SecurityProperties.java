package com.paranietharan.byteblog.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @NotEmpty
    private List<String> allowedOrigins = new ArrayList<>();
}
