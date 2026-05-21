package com.akgeneralstore.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI akGeneralStoreOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("AK General Store API")
                        .description("Starter backend APIs for customer, admin, and delivery modules.")
                        .version("v1")
                        .contact(new Contact().name("AK General Store"))
        );
    }
}
