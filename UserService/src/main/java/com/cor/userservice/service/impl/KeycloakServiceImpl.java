package com.cor.userservice.service.impl;

import com.cor.userservice.config.KeycloakConfig;
import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.dto.UserProfileKeycloakDto;
import com.cor.userservice.util.enam.UserRole;
import com.cor.userservice.util.exception.KeycloakOperationException;
import com.cor.userservice.util.exception.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;


/**
 * Сервис для взаимодействия с Keycloak.
 */
@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KeycloakServiceImpl {

    Keycloak keycloak;
    String realm;
    KeycloakConfig config;
    WebClient keycloakWebClient;

    /**
     * Конструктор сервиса Keycloak.
     *
     * @param keycloak Клиент Keycloak
     * @param config   Конфигурация Keycloak
     */
    @Autowired
    public KeycloakServiceImpl(Keycloak keycloak, KeycloakConfig config, KeycloakConfig config1, WebClient keycloakWebClient) {
        this.keycloak = keycloak;
        this.realm = config.getRealm();
        this.config = config1;
        this.keycloakWebClient = keycloakWebClient;
    }

    /**
     * Создание пользователя в Keycloak
     *
     * @param requestDto DTO с данными для регистрации
     * @return UUID созданного пользователя
     * @throws UserAlreadyExistsException если пользователь уже существует
     * @throws KeycloakOperationException если возникла ошибка при создании пользователя
     */
    public UUID createUser(UserProfileKeycloakDto requestDto) {
        log.info("Создание пользователя в Keycloak: {}", requestDto.getUsername());
        RealmResource realmResource = keycloak.realm(realm);

        log.info("Проверка существования пользователя по username");
        List<UserRepresentation> existingUsers = realmResource.users()
                .searchByUsername(requestDto.getUsername(), true);
        if (!existingUsers.isEmpty()) {
            log.info("Пользователь с username '{}' уже существует", requestDto.getUsername());
            throw new UserAlreadyExistsException("username или email", requestDto.getUsername());
        }

        log.info("Проверка существования пользователя по email");
        existingUsers = realmResource.users().searchByEmail(requestDto.getEmail(), true);
        if (!existingUsers.isEmpty()) {
            log.info("Пользователь с email '{}' уже существует", requestDto.getEmail());
            throw new UserAlreadyExistsException("username или email", requestDto.getUsername());
        }

        UserRepresentation user = buildUserRepresentation(requestDto);

        log.info("Отправка запроса на создание пользователя в Keycloak");
        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() == 201) {
                String location = response.getLocation().getPath();
                String keycloakId = location.substring(location.lastIndexOf('/') + 1);
                UUID userId = UUID.fromString(keycloakId);
                log.info("Пользователь успешно создан в Keycloak с ID: {}", userId);
                return userId;
            } else if (response.getStatus() == 409) {
                log.info("Конфликт при создании пользователя: {}", requestDto.getUsername());
                throw new UserAlreadyExistsException("username или email", requestDto.getUsername());
            } else {
                log.info("Ошибка создания пользователя. Код ответа: {}", response.getStatus());
                throw new KeycloakOperationException("Ошибка создания пользователя: " + response.getStatusInfo());
            }
        } catch (Exception ex) {
            log.info("Критическая ошибка при создании пользователя в Keycloak", ex);
            throw new KeycloakOperationException("Ошибка при работе с Keycloak", ex);
        }
    }

    /**
     * Создание объекта UserRepresentation для Keycloak
     *
     * @param requestDto DTO с данными для регистрации
     * @return UserRepresentation для Keycloak
     */
    private UserRepresentation buildUserRepresentation(UserProfileKeycloakDto requestDto) {
        log.info("Формирование UserRepresentation для пользователя {}", requestDto.getUsername());
        UserRepresentation user = new UserRepresentation();
        user.setUsername(requestDto.getUsername());
        user.setEmail(requestDto.getEmail());
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(requestDto.getPassword());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        return user;
    }

    /**
     * Назначение роли USER пользователю
     *
     * @param userId ID пользователя
     * @throws KeycloakOperationException если возникла ошибка при назначении роли
     */
    public void assignUserRole(UUID userId) {
        log.info("Назначение роли USER пользователю {}", userId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            RoleRepresentation userRole = realmResource.roles().get("USER").toRepresentation();
            realmResource.users().get(userId.toString()).roles().realmLevel().add(List.of(userRole));
            log.info("Роль USER успешно назначена пользователю {}", userId);
        } catch (Exception ex) {
            log.info("Ошибка назначения роли пользователю {}: {}", userId, ex.getMessage());
            throw new KeycloakOperationException("Ошибка назначения роли", ex);
        }
    }

    /**
     * Обновление пользователя в Keycloak
     *
     * @param userId ID пользователя
     * @param userProfileDto DTO с обновленными данными
     * @return обновленный UserProfileDto
     * @throws KeycloakOperationException если возникла ошибка при обновлении
     */
    public void updateUser(UUID userId, UserProfileKeycloakDto userProfileDto) {
        log.info("Обновление пользователя {} в Keycloak", userId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserRepresentation user = realmResource.users().get(userId.toString()).toRepresentation();

            // Обновляем основные поля
            user.setFirstName(userProfileDto.getFirstName());
            user.setLastName(userProfileDto.getLastName());
            user.setEmail(userProfileDto.getEmail());

            // Обновляем username если он изменился
            if (userProfileDto.getUsername() != null && !userProfileDto.getUsername().equals(user.getUsername())) {
                user.setUsername(userProfileDto.getUsername());
            }

            // Обновляем пароль если он предоставлен
            if (userProfileDto.getPassword() != null && !userProfileDto.getPassword().trim().isEmpty()) {
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(userProfileDto.getPassword());
                credential.setTemporary(false);
                user.setCredentials(List.of(credential));
            }

            realmResource.users().get(userId.toString()).update(user);
            log.info("Пользователь {} успешно обновлен в Keycloak", userId);

            UserProfileDto result = new UserProfileDto();
            result.setFirstName(userProfileDto.getFirstName());
            result.setLastName(userProfileDto.getLastName());
            result.setEmail(userProfileDto.getEmail());
            result.setUsername(userProfileDto.getUsername());

        } catch (Exception ex) {
            log.error("Ошибка обновления пользователя {}: {}", userId, ex.getMessage());
            throw new KeycloakOperationException("Ошибка обновления пользователя", ex);
        }
    }

    /**
     * Удаление пользователя из Keycloak
     *
     * @param userId ID пользователя для удаления
     */
    public void deleteUser(UUID userId) {
        log.info("Попытка удаления пользователя {}", userId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            realmResource.users().get(userId.toString()).remove();
            log.info("Пользователь {} успешно удален из Keycloak", userId);
        } catch (Exception ex) {
            log.info("Ошибка удаления пользователя {}: {}", userId, ex.getMessage());
        }
    }

    private String getUsernameById(UUID userId) {
        UserRepresentation user = keycloak.realm(realm).users().get(userId.toString()).toRepresentation();
        return user.getUsername();
    }

    /**
     * Получение роли пользователя из Keycloak
     *
     * @param keycloakId ID пользователя в Keycloak
     * @return роль пользователя или null если не найден
     * @throws KeycloakOperationException если возникла ошибка при получении роли
     */
    public UserRole getUserRole(String keycloakId) {
        log.debug("Получение роли пользователя {} из Keycloak", keycloakId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(keycloakId);

            List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();

            for (RoleRepresentation role : roles) {
                UserRole userRole = UserRole.fromString(role.getName());
                if (userRole != null) {
                    return userRole;
                }
            }

            return UserRole.USER;

        } catch (Exception ex) {
            log.error("Ошибка при получении роли пользователя {} из Keycloak", keycloakId, ex);
            throw new KeycloakOperationException("Не удалось получить роль пользователя из Keycloak", ex);
        }
    }

    /**
     * Изменение роли пользователя в Keycloak
     *
     * @param keycloakId ID пользователя в Keycloak
     * @param newRole новая роль
     * @throws KeycloakOperationException если возникла ошибка при изменении роли
     */
    public void changeUserRole(String keycloakId, UserRole newRole) {
        log.info("Изменение роли пользователя {} на {} в Keycloak", keycloakId, newRole);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(keycloakId);

            // Удаляем все текущие роли
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            for (RoleRepresentation role : currentRoles) {
                userResource.roles().realmLevel().remove(List.of(role));
            }

            // Назначаем новую роль
            RoleRepresentation newRoleRep = realmResource.roles().get(newRole.getRoleName()).toRepresentation();
            userResource.roles().realmLevel().add(List.of(newRoleRep));

            log.info("Роль пользователя {} успешно изменена на {}", keycloakId, newRole);

        } catch (Exception ex) {
            log.error("Ошибка при изменении роли пользователя {} в Keycloak", keycloakId, ex);
            throw new KeycloakOperationException("Не удалось изменить роль пользователя в Keycloak", ex);
        }
    }

    /**
     * Назначение конкретной роли пользователю
     *
     * @param userId ID пользователя
     * @param role роль для назначения
     * @throws KeycloakOperationException если возникла ошибка при назначении роли
     */
    public void assignRole(UUID userId, UserRole role) {
        log.info("Назначение роли {} пользователю {}", role, userId);
        try {
            RealmResource realmResource = keycloak.realm(realm);
            RoleRepresentation roleRepresentation = realmResource.roles().get(role.getRoleName()).toRepresentation();
            realmResource.users().get(userId.toString()).roles().realmLevel().add(List.of(roleRepresentation));
            log.info("Роль {} успешно назначена пользователю {}", role, userId);
        } catch (Exception ex) {
            log.error("Ошибка назначения роли {} пользователю {}: {}", role, userId, ex.getMessage());
            throw new KeycloakOperationException("Ошибка назначения роли", ex);
        }
    }
}