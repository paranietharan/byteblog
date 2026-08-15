package com.paranietharan.byteblog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Byteblog API",
                version = "v1",
                description = "REST API for authentication, publishing, comments, likes, tags, and moderation.",
                contact = @Contact(name = "Byteblog API team"),
                license = @License(name = "Private project")
        ),
        servers = {
                @Server(url = "/", description = "Current server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access token returned by registration, login, or refresh."
)
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer standardErrorResponses() {
        return openApi -> {
            ObjectSchema errorSchema = new ObjectSchema();
            errorSchema.addProperty("timestamp", new DateTimeSchema());
            errorSchema.addProperty("status", new IntegerSchema().example(400));
            errorSchema.addProperty("error", new StringSchema().example("Validation Error"));
            errorSchema.addProperty("message", new StringSchema().example("password: Password must be between 12 and 100 characters"));
            errorSchema.addProperty("path", new StringSchema().example("/api/v1/auth/register"));
            openApi.getComponents().addSchemas("ErrorResponse", errorSchema);

            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                addErrorResponse(responses, "400", "Invalid request");
                addErrorResponse(responses, "401", "Authentication required or token invalid");
                addErrorResponse(responses, "403", "Insufficient permission");
                addErrorResponse(responses, "409", "Concurrent update or uniqueness conflict");
                addErrorResponse(responses, "429", "Rate limit exceeded");
                addErrorResponse(responses, "500", "Unexpected server error");
            }));
        };
    }

    private void addErrorResponse(ApiResponses responses, String code, String description) {
        if (!responses.containsKey(code)) {
            responses.addApiResponse(code, new ApiResponse()
                    .description(description)
                    .content(new io.swagger.v3.oas.models.media.Content().addMediaType(
                            "application/json",
                            new io.swagger.v3.oas.models.media.MediaType()
                                    .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ErrorResponse"))
                    )));
        }
    }
}
