package com.mypetadmin.ps_login.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI psLoginOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PS_Login API")
                .version("v1")
                .description("Credenciais e autenticação do My Pet Admin"));
    }
}
