package com.cor.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Set;

/**
 * DTO (Data Transfer Object) для передачи данных профиля пользователя между слоями приложения.
 * Используется для сериализации/десериализации данных в REST API.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "DTO создания профиля пользователя")
public class UserProfileCreateDto {

    /**
     * Уникальный идентификатор пользователя в Keycloak.
     */
    @Schema(
            description = "Уникальный идентификатор пользователя в Keycloak",
            example = "123e4567-e89b-12d3-a456-426614174000",
            required = true
    )
    @NotBlank(message = "Идентификатор пользователя не может быть пустым")
    String keycloakId;

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Schema(
            description = "Уникальное имя пользователя для входа в систему",
            example = "john_doe",
            required = true
    )
    @NotBlank(message = "Имя пользователя не может быть пустым")
    String username;

    /**
     * Email адрес пользователя.
     */
    @Schema(
            description = "Email адрес пользователя",
            example = "user@example.com",
            required = true
    )
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Email должен быть валидным")
    String email;

    /**
     * Набор ролей пользователя в системе.
     */
    @Schema(
            description = "Набор ролей пользователя в системе",
            example = "[\"USER\", \"ADMIN\"]",
            required = true
    )
    @NotNull(message = "Роли пользователя не могут быть null")
    Set<String> roles;
}
