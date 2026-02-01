package com.cor.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI (Swagger) для документирования API.
 * 
 * <p>Этот класс настраивает основные параметры документации API,
 * включая название, описание, версию и контактную информацию.</p>
 * 
 * @author Your Name
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Создает и настраивает бин OpenAPI с основной информацией о приложении.
     * 
     * @return настроенный объект OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("API для управления профилями пользователей")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("SRS")));
    }
}
