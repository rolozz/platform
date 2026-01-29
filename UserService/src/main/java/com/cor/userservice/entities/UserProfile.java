package com.cor.userservice.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
 * Сущность, представляющая профиль пользователя в системе.
 * Хранит основную информацию о пользователе, включая учетные данные и роли.
 * 
 * <p>Эта сущность отображается на таблицу {@code user_profiles} в базе данных.
 * Основным ключом является {@code keycloakId}, который соответствует идентификатору
 * пользователя в системе аутентификации Keycloak.</p>
 * 
 * <p>Класс использует Lombok для автоматической генерации геттеров, сеттеров
 * и стандартных конструкторов, что сокращает количество шаблонного кода.</p>
 * 
 * @author Your Name
 * @version 1.0
 * @since 1.0
 */
@Entity
@Table(name = "user_profiles")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfile {

    /**
     * Уникальный идентификатор пользователя в системе Keycloak.
     * Используется в качестве первичного ключа в таблице.
     */
    @Id
    @Column(name = "keycloak_id")
    String keycloakId;

    /**
     * Уникальное имя пользователя для входа в систему.
     * Должно быть уникальным для каждого пользователя.
     */
    @Column(name = "username", unique = true)
    String username;

    /**
     * Email адрес пользователя.
     * Используется для уведомлений и восстановления пароля.
     * Должен быть уникальным для каждого пользователя.
     */
    @Column(name = "email", unique = true)
    String email;

    /**
     * Набор ролей пользователя в системе.
     * Определяет права доступа пользователя к различным ресурсам.
     * Может содержать несколько ролей одновременно.
     */
    @Column(name = "role")
    Set<String> roles;

    /**
     * Дата и время создания записи (устанавливается автоматически)
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    /**
     * Дата и время последнего обновления записи (обновляется автоматически)
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
