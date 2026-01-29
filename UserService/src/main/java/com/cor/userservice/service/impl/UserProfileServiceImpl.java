package com.cor.userservice.service.impl;

import com.cor.userservice.dto.UserProfileCreateDto;
import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.entities.UserProfile;
import com.cor.userservice.mapper.UserProfileMapper;
import com.cor.userservice.repository.UserProfileRepository;
import com.cor.userservice.service.UserProfileService;
import com.cor.userservice.util.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса для работы с профилями пользователей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;

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
    @Transactional
    public UserProfileDto createUserProfile(UserProfileCreateDto userProfileCreateDto) {
        log.debug("Создание нового профиля пользователя: {}", userProfileCreateDto.getEmail());

        if (userProfileRepository.existsByEmail(userProfileCreateDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileCreateDto.getEmail() + " уже существует");
        }

        if (userProfileRepository.existsByUsername(userProfileCreateDto.getUsername())) {
            throw new IllegalArgumentException("Пользователь с username " + userProfileCreateDto.getUsername() + " уже существует");
        }

        UserProfile userProfile = userProfileMapper.toEntity(userProfileCreateDto);
        UserProfile savedProfile = userProfileRepository.save(userProfile);
        log.info("Создан новый профиль пользователя с ID: {}", savedProfile.getKeycloakId());

        return userProfileMapper.toDto(savedProfile);
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(String keycloakId, UserProfileDto userProfileDto) {
        log.debug("Обновление профиля пользователя с ID: {}", keycloakId);

        UserProfile existingProfile = userProfileRepository.findById(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Профиль пользователя не найден с ID: " + keycloakId));

        if (!existingProfile.getEmail().equals(userProfileDto.getEmail())
                && userProfileRepository.existsByEmail(userProfileDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileDto.getEmail() + " уже существует");
        }
        UserProfile updatedProfile = userProfileMapper.updateEntityFromDto(userProfileDto, existingProfile);
        log.info("Обновлен профиль пользователя с ID: {}", keycloakId);

        return userProfileMapper.toDto(userProfileMapper.updateEntityFromDto(userProfileDto, updatedProfile));
    }

    @Override
    @Transactional
    public void deleteUserProfile(String keycloakId) {
        log.debug("Удаление профиля пользователя с ID: {}", keycloakId);

        if (!userProfileRepository.existsById(keycloakId)) {
            throw new ResourceNotFoundException("Профиль пользователя не найден с ID: " + keycloakId);
        }

        userProfileRepository.deleteById(keycloakId);
        log.info("Удален профиль пользователя с ID: {}", keycloakId);
    }
}
