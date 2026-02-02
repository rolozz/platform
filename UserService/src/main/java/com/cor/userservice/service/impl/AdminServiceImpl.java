package com.cor.userservice.service.impl;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.repository.UserProfileRepository;
import com.cor.userservice.service.AdminService;
import com.cor.userservice.service.UserProfileService;
import com.cor.userservice.util.enam.UserRole;
import com.cor.userservice.util.exception.InsufficientPermissionsException;
import com.cor.userservice.util.exception.KeycloakOperationException;
import com.cor.userservice.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public void deleteUserByUsername(String username, String adminUsername) {
        log.info("Администратор {} пытается удалить пользователя {}", adminUsername, username);

        if (!userProfileRepository.existsByUsername(adminUsername)) {
            throw new ResourceNotFoundException("Администратор с username " + adminUsername + " не найден");
        }

        UserRole adminRole = getUserRole(adminUsername);
        if (adminRole == null || adminRole == UserRole.USER) {
            throw new InsufficientPermissionsException("Пользователь " + adminUsername + " не имеет прав на удаление пользователей");
        }

        var userProfile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с username " + username + " не найден"));

        UserRole targetRole = getUserRole(username);
        if (targetRole == null) {
            targetRole = UserRole.USER;
        }

        if (!UserRole.canManageRole(adminRole, targetRole)) {
            throw new InsufficientPermissionsException("Администратор " + adminUsername + " не может удалить пользователя с ролью " + targetRole);
        }

        try {
            UUID userId = UUID.fromString(userProfile.getKeycloakId());
            keycloakService.deleteUser(userId);

            userProfileRepository.delete(userProfile);

            log.info("Пользователь {} успешно удален администратором {}", username, adminUsername);

        } catch (Exception ex) {
            log.error("Ошибка при удалении пользователя {}", username, ex);
            throw new KeycloakOperationException("Не удалось удалить пользователя", ex);
        }
    }

    @Override
    public UserProfileDto promoteUser(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} пытается повысить роль пользователя {} до {}", adminUsername, username, newRole);

        return changeUserRole(username, newRole, adminUsername);
    }

    @Override
    public UserProfileDto demoteUser(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} пытается понизить роль пользователя {} до {}", adminUsername, username, newRole);

        return changeUserRole(username, newRole, adminUsername);
    }

    @Override
    public UserProfileDto changeUserRole(String username, UserRole newRole, String adminUsername) {
        log.info("Администратор {} изменяет роль пользователя {} на {}", adminUsername, username, newRole);

        UserRole adminRole = getUserRole(adminUsername);
        if (adminRole == null || adminRole == UserRole.USER) {
            throw new InsufficientPermissionsException("Пользователь " + adminUsername + " не имеет прав на изменение ролей");
        }

        var userProfile = userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь с username " + username + " не найден"));

        UserRole currentTargetRole = getUserRole(username);

        if (!UserRole.canManageRole(adminRole, currentTargetRole)) {
            throw new InsufficientPermissionsException("Администратор " + adminUsername + " не может изменять роль пользователя с ролью " + currentTargetRole);
        }

        if (!UserRole.canManageRole(adminRole, newRole)) {
            throw new InsufficientPermissionsException("Администратор " + adminUsername + " не может назначить роль " + newRole);
        }

        try {
            keycloakService.changeUserRole(userProfile.getKeycloakId(), newRole);

            log.info("Роль пользователя {} изменена на {}", username, newRole);

            return userProfileService.getUserProfile(userProfile.getKeycloakId());

        } catch (Exception ex) {
            log.error("Ошибка при изменении роли пользователя {}", username, ex);
            throw new KeycloakOperationException("Не удалось изменить роль пользователя", ex);
        }
    }

    @Override
    public UserRole getUserRole(String username) {
        log.debug("Получение роли пользователя {}", username);

        var userProfile = userProfileRepository.findByUsername(username)
                .orElse(null);

        if (userProfile == null) {
            log.warn("Пользователь с username {} не найден в базе данных", username);
            return null;
        }

        return keycloakService.getUserRole(userProfile.getKeycloakId());
    }
}
