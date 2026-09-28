package thickethub.config;

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

    private static final String SCHEMA_SEGURANCA = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ThickeThub API")
                        .description("API do sistema de chamados da Defensoria Pública de MG")
                        .version("1.0.0")
                        .contact(new Contact().name("Equipe de TI - Defensoria Pública MG")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_SEGURANCA))
                .components(new Components()
                        .addSecuritySchemes(SCHEMA_SEGURANCA,
                                new SecurityScheme()
                                        .name(SCHEMA_SEGURANCA)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Cole o accessToken obtido em POST /auth/login")));
    }
}