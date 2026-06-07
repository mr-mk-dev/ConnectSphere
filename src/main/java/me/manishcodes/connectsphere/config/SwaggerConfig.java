package me.manishcodes.connectsphere.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectSphere API")
                        .description("""
                                REST API for ConnectSphere — a social media platform.
                                
                                **Authentication:** Most endpoints require a JWT Bearer token.
                                1. Register → `POST /api/v1/auth/register`
                                2. Login → `POST /api/v1/auth/login` — copy the `token` from the response
                                3. Click **Authorize** (🔒) above and paste: `Bearer <your-token>`
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Manish")
                                .url("https://github.com/manishcodes")))

                // Global security — every endpoint requires JWT unless marked @SecurityRequirements({})
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))

                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
