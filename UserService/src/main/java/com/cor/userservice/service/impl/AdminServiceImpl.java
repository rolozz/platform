package com.cor.userservice.service.impl;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.repository.UserProfileRepository;
import com.cor.userservice.service.AdminService;
import com.cor.userservice.service.UserProfileService;
import com.cor.userservice.util.enam.UserRole;
import com.cor.userservice.util.exception.ResourceNotFoundException;
import com.cor.userservice.util.exception.KeycloakOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Реализация сервиса для административных функций управления пользователями.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final KeycloakServiceImpl keycloakService;
    private final Keycloak keycloak;
    private final String realm = "my-realm";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserByUsername(String username, String adminUsername) {
        log.info("Администратор {} пытается удалить пользователя {}", adminUsername, username);
        
        // Проверяем права администратора
        UserRole adminRole = getUserRole(adminUsername);
        if (adminRole == null || adminRole == UserRole.USER) {
            throw new SecurityException("Пользователь " + adminUsername + " не имеет прав на удаление пользователей");
        }
        
        // Находим пользователя в БД
        var userProfile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с username " + username + " не найден"));
        
        // Проверяем права на удаление (нельзя удалять равных или выше по статусу)
        UserRole targetRole = getUserRole(username);
        if (!UserRole.canManageRole(adminRole, targetRole)) {
            throw new SecurityException("Администратор " + adminUsername + " не может удалить пользователя с ролью " + targetRole);
        }
        
        try {
            // Удаляем из Keycloak
            UUID userId = UUID.fromString(userProfile.getKeycloakId());
            keycloakService.deleteUser(userId);
            
            // Удаляем из БД
            userProfileRepository.delete(userProfile);
            
            log.info("Пользователь {} успешно удален администратором {}", username, adminUsername);
            
        } catch (Exception ex) {
            log.error("Ошибка при удалении пользователя {}", username, ex);
            throw new KeycloakOperationException("Не удалось удалить пользователя", ex);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto promoteUser(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} пытается повысить роль пользователя {} до {}", adminUsername, username, newRole);
        
        return changeUserRole(username, newRole, adminUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto demoteUser(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} пытается понизить роль пользователя {} до {}", adminUsername, username, newRole);
        
        return changeUserRole(username, newRole, adminUsername);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto changeUserRole(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} изменяет роль пользователя {} на {}", adminUsername, username, newRole);
        
        // Проверяем права администратора
        UserRole adminRole = getUserRole(adminUsername);
        if (adminRole == null || adminRole == UserRole.USER) {
            throw new SecurityException("Пользователь " + adminUsername + " не имеет прав на изменение ролей");
        }
        
        // Находим пользователя
        var userProfile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с username " + username + " не найден"));
        
        // Получаем текущую роль целевого пользователя
        UserRole currentTargetRole = getUserRole(username);
        
        // Проверяем права на изменение роли
        if (!UserRole.canManageRole(adminRole, currentTargetRole)) {
            throw new SecurityException("Администратор " + adminUsername + " не может изменять роль пользователя с ролью " + currentTargetRole);
        }
        
        // Проверяем может ли администратор назначить новую роль
        if (!UserRole.canManageRole(adminRole, newRole)) {
            throw new SecurityException("Администратор " + adminUsername + " не может назначить роль " + newRole);
        }
        
        try {
            // Получаем пользователя в Keycloak
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userProfile.getKeycloakId());
            
            // Удаляем все текущие роли realm
            List<RoleRepresentation> currentRoles = userResource.roles().realmLevel().listAll();
            for (RoleRepresentation role : currentRoles) {
                userResource.roles().realmLevel().remove(List.of(role));
            }
            
            // Назначаем новую роль
            RoleRepresentation newRoleRep = realmResource.roles().get(newRole.getRoleName()).toRepresentation();
            userResource.roles().realmLevel().add(List.of(newRoleRep));
            
            log.info("Роль пользователя {} изменена с {} на {}", username, currentTargetRole, newRole);
            
            // Возвращаем обновленный профиль
            return userProfileService.getUserProfile(userProfile.getKeycloakId());
            
        } catch (Exception ex) {
            log.error("Ошибка при изменении роли пользователя {}", username, ex);
            throw new KeycloakOperationException("Не удалось изменить роль пользователя", ex);
        }
    }

    @Override
    public UserRole getUserRole(String username) {
        log.debug("Получение роли пользователя {}", username);
        
        try {
            // Находим пользователя в БД чтобы получить keycloakId
            var userProfile = userProfileRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("Пользователь с username " + username + " не найден"));
            
            // Получаем пользователя в Keycloak
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            UserResource userResource = usersResource.get(userProfile.getKeycloakId());
            
            // Получаем роли пользователя
            List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();
            
            // Ищем среди ролей наши системные роли
            for (RoleRepresentation role : roles) {
                UserRole userRole = UserRole.fromString(role.getName());
                if (userRole != null) {
                    return userRole;
                }
            }
            
            // Если системных ролей нет, считаем пользователем
            return UserRole.USER;
            
        } catch (Exception ex) {
            log.error("Ошибка при получении роли пользователя {}", username, ex);
            throw new KeycloakOperationException("Не удалось получить роль пользователя", ex);
        }
    }
}
