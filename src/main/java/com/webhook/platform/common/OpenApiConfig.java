package com.webhook.platform.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String API_KEY_SECURITY_SCHEME = "ApiKeyAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Reliable Webhook Delivery Platform API")
                        .version("1.0.0")
                        .description("High-performance webhook ingestion, HMAC payload signing, asynchronous virtual-thread delivery, exponential retry engine, and dead-letter queue management.")
                        .contact(new Contact().name("Webhook Platform Team")))
                .addSecurityItem(new SecurityRequirement().addList(API_KEY_SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(API_KEY_SECURITY_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-API-Key")
                                        .description("Provide your API Key in the X-API-Key header or Authorization: Bearer <key>")));
    }
}

