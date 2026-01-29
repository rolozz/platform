package com.cor.userservice.service.impl;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.entities.UserProfile;
import com.cor.userservice.util.exception.ResourceNotFoundException;
import com.cor.userservice.mapper.UserProfileMapper;
import com.cor.userservice.repository.UserProfileRepository;
import com.cor.userservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<UserProfileDto> getAllUserProfiles() {
        log.debug("Получение списка всех профилей пользователей");
        return userProfileRepository.findAll().stream()
                .map(userProfileMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserProfileDto createUserProfile(UserProfileDto userProfileDto) {
        log.debug("Создание нового профиля пользователя: {}", userProfileDto.getEmail());
        
        if (userProfileRepository.existsByEmail(userProfileDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileDto.getEmail() + " уже существует");
        }
        
        if (userProfileRepository.existsByUsername(userProfileDto.getUsername())) {
            throw new IllegalArgumentException("Пользователь с username " + userProfileDto.getUsername() + " уже существует");
        }
        
        UserProfile userProfile = userProfileMapper.toEntity(userProfileDto);
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
        
        // Проверяем, что email не занят другим пользователем
        if (!existingProfile.getEmail().equals(userProfileDto.getEmail()) 
                && userProfileRepository.existsByEmail(userProfileDto.getEmail())) {
            throw new IllegalArgumentException("Пользователь с email " + userProfileDto.getEmail() + " уже существует");
        }
        
        // Обновляем данные профиля
        existingProfile.setUsername(userProfileDto.getUsername());
        existingProfile.setEmail(userProfileDto.getEmail());
        existingProfile.setRoles(userProfileDto.getRoles());
        
        UserProfile updatedProfile = userProfileRepository.save(existingProfile);
        log.info("Обновлен профиль пользователя с ID: {}", keycloakId);
        
        return userProfileMapper.toDto(updatedProfile);
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
