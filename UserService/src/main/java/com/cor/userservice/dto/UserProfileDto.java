package com.cor.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
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
@Schema(description = "DTO профиля пользователя")
public class UserProfileDto {

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Schema(
        description = "Уникальное имя пользователя для входа в систему",
        example = "john_doe"
    )
    String username;

    /**
     * Email адрес пользователя.
     */
    @Schema(
        description = "Email адрес пользователя",
        example = "user@example.com"
    )
    String email;

    /**
     * Имя пользователя.
     */
    @Schema(
            description = "Имя пользователя",
            example = "Вася"
    )
    String firstName;

    /**
     * Фамилия пользователя.
     */
    @Schema(
            description = "Фамилия пользователя",
            example = "Васин"
    )
    String lastName;

    /**
     * Дата и время создания записи (устанавливается автоматически)
     */
    @Schema(
            description = "время создания"
    )
    LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи (обновляется автоматически)
     */
    @Schema(
            description = "время обновления"
    )
    LocalDateTime updatedAt;
}
