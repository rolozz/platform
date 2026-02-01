package com.cor.userservice.service.impl;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.dto.UserProfileKeycloakDto;
import com.cor.userservice.entities.UserProfile;
import com.cor.userservice.mapper.UserProfileMapper;
import com.cor.userservice.repository.UserProfileRepository;
import com.cor.userservice.service.UserProfileService;
import com.cor.userservice.util.exception.KeycloakOperationException;
import com.cor.userservice.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Реализация сервиса для работы с профилями пользователей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final KeycloakServiceImpl keycloakService;

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(String keycloakId) {
        log.debug("Получение профиля пользователя с ID: {}", keycloakId);
        return userProfileRepository.findById(keycloakId)
                .map(userProfileMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль пользователя не найден с ID: " + keycloakId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileDto> getAllUserProfiles(Pageable pageable) {
        log.debug("Получение профилей пользователей с пагинацией: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return userProfileRepository.findAll(pageable)
                .map(userProfileMapper::toDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileDto createUserProfile(UserProfileKeycloakDto userProfileCreateDto) {
        log.debug("Создание нового профиля пользователя: {}", userProfileCreateDto.getEmail());

        if (userProfileRepository.existsByEmail(userProfileCreateDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileCreateDto.getEmail() + " уже существует");
        }

        if (userProfileRepository.existsByUsername(userProfileCreateDto.getUsername())) {
            throw new IllegalArgumentException("Пользователь с username " + userProfileCreateDto.getUsername() + " уже существует");
        }

        try {
            UUID keycloakId = keycloakService.createUser(userProfileCreateDto);

            keycloakService.assignUserRole(keycloakId);

            UserProfile userProfile = userProfileMapper.toEntity(userProfileCreateDto);
            userProfile.setKeycloakId(keycloakId.toString());
            UserProfile savedProfile = userProfileRepository.save(userProfile);

            log.info("Создан новый профиль пользователя с ID: {}", savedProfile.getKeycloakId());
            return userProfileMapper.toDto(savedProfile);

        } catch (Exception ex) {
            log.error("Ошибка при создании пользователя в Keycloak, откат транзакции", ex);
            throw new KeycloakOperationException("Не удалось создать пользователя в Keycloak", ex);
        }
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(String keycloakId, UserProfileKeycloakDto userProfileDto) {
        log.debug("Обновление профиля пользователя с ID: {}", keycloakId);

        UserProfile existingProfile = userProfileRepository.findById(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль пользователя не найден с ID: " + keycloakId));

        if (!existingProfile.getEmail().equals(userProfileDto.getEmail())
                && userProfileRepository.existsByEmail(userProfileDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileDto.getEmail() + " уже существует");
        }

        if (!existingProfile.getUsername().equals(userProfileDto.getUsername())
                && userProfileRepository.existsByUsername(userProfileDto.getUsername())) {
            throw new IllegalArgumentException("Пользователь с username " + userProfileDto.getUsername() + " уже существует");
        }

        if (userProfileDto.getPassword() != null && !userProfileDto.getPassword().trim().isEmpty()
                && !userProfileDto.isPasswordConfirmed()) {
            throw new IllegalArgumentException("Пароль и подтверждение пароля не совпадают");
        }

        try {
            UUID userId = UUID.fromString(keycloakId);

            keycloakService.updateUser(userId, userProfileDto);

            UserProfile updatedProfile = userProfileMapper.updateEntityFromDto(userProfileDto, existingProfile);
            UserProfile savedProfile = userProfileRepository.save(updatedProfile);

            log.info("Обновлен профиль пользователя с ID: {}", keycloakId);
            return userProfileMapper.toDto(savedProfile);

        } catch (Exception ex) {
            log.error("Ошибка при обновлении пользователя", ex);
            throw new KeycloakOperationException("Не удалось обновить пользователя", ex);
        }
    }

    @Override
    @Transactional
    public void deleteUserProfile(String keycloakId) {
        log.debug("Удаление профиля пользователя с ID: {}", keycloakId);

        if (!userProfileRepository.existsById(keycloakId)) {
            throw new ResourceNotFoundException("Профиль пользователя не найден с ID: " + keycloakId);
        }

        try {
            UUID userId = UUID.fromString(keycloakId);
            keycloakService.deleteUser(userId);

            userProfileRepository.deleteById(keycloakId);

            log.info("Удален профиль пользователя с ID: {} из Keycloak и базы данных", keycloakId);

        } catch (Exception ex) {
            log.error("Ошибка при удалении пользователя", ex);
            throw new KeycloakOperationException("Не удалось удалить пользователя", ex);
        }
    }
}
