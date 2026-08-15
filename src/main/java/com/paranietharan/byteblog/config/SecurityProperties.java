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
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {
        private boolean enabled = true;
        private int loginLimit = 10;
        private int loginWindowSeconds = 60;
        private int registrationLimit = 5;
        private int registrationWindowSeconds = 3600;
        private int refreshLimit = 20;
        private int refreshWindowSeconds = 60;
        private int verificationLimit = 20;
        private int verificationWindowSeconds = 3600;
    }
}
