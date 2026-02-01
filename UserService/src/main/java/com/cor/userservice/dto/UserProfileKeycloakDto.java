package com.cor.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

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
public class UserProfileKeycloakDto {

    /**
     * Уникальное имя пользователя для входа в систему.
     */
    @Schema(
            description = "Уникальное имя пользователя для входа в систему",
            example = "john_doe",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Имя пользователя не может быть пустым")
    String username;

    /**
     * Email адрес пользователя.
     */
    @Schema(
            description = "Email адрес пользователя",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Email должен быть валидным")
    String email;

    /**
     * Имя пользователя.
     */
    @Schema(
            description = "Имя пользователя",
            example = "Вася",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String firstName;

    /**
     * Фамилия пользователя.
     */
    @Schema(
            description = "Фамилия пользователя",
            example = "Васин",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank
    String lastName;

    /**
     * пароль.
     * от 8 символов.
     */
    @Schema(
            description = "пароль.",
            example = "SecurePass123!",
            minLength = 8,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "не может пароль быть пустым")
    @Size(min = 8, message = "миниму 8 символов")
    String password;

    /**
     * подтвержение пароля.
     * должны совпадать.
     */
    @Schema(
            description = "подтверждение пароля",
            example = "SecurePass123!",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "подтверждение пароля не может быть пустым")
    String confirmPassword;

    /**
     * Verifies if password and confirmation match.
     *
     * @return true if passwords match, false otherwise
     */
    public boolean isPasswordConfirmed() {
        return Objects.equals(password, confirmPassword);
    }
}
