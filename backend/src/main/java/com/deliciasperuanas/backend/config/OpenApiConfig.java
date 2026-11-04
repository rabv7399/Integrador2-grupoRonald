package com.deliciasperuanas.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliciasPeruanasApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Delicias Peruanas API")
                        .version("APF3")
                        .description("API REST de autenticación, categorías, productos, mesas y reservas. "
                                + "Las operaciones protegidas requieren JWT y el rol correspondiente."))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer autenticacionPublica() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, item) -> {
                    if (path.startsWith("/api/auth/")) {
                        item.readOperations().forEach(operation -> operation.setSecurity(List.of()));
                    }
                });
            }
        };
    }
}
