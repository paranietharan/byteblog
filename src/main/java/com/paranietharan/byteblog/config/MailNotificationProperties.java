package com.paranietharan.byteblog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailNotificationProperties {
    private boolean enabled;
    private String from;
    private String fromName = "Byteblog";
    private String apiBaseUrl;
    private String frontendBaseUrl;
    private int outboxBatchSize = 20;
    private int outboxMaxAttempts = 5;
    private int outboxProcessingTimeoutMinutes = 15;
}
