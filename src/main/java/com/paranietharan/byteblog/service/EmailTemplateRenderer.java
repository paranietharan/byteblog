package com.paranietharan.byteblog.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailTemplateRenderer {

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String render(String templateName, Map<String, String> values) {
        String rendered = templateCache.computeIfAbsent(templateName, this::loadTemplate);
        for (Map.Entry<String, String> value : values.entrySet()) {
            rendered = rendered.replace("{{" + value.getKey() + "}}", escapeHtml(value.getValue()));
        }
        return rendered;
    }

    private String loadTemplate(String templateName) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email/" + templateName + ".html");
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load email template: " + templateName, exception);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
