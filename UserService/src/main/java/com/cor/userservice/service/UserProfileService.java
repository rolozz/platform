package com.cor.userservice.service;

import com.cor.userservice.dto.UserProfileCreateDto;
import com.cor.userservice.dto.UserProfileDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Сервис для работы с профилями пользователей.
 */
public interface UserProfileService {
    
    /**
     * Получает профиль пользователя по его идентификатору.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @return DTO профиля пользователя
     */
    UserProfileDto getUserProfile(String keycloakId);
    
    /**
     * Получает пэйдж всех профилей пользователей.
     *
     * @return список DTO профилей пользователей
     */
    Page<UserProfileDto> getAllUserProfiles(Pageable pageable);
    
    /**
     * Создает новый профиль пользователя.
     *
     * @param userProfileDto DTO с данными нового профиля
     * @return DTO созданного профиля
     */
    UserProfileDto createUserProfile(UserProfileCreateDto userProfileCreateDtoDto);
    
    /**
     * Обновляет существующий профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @param userProfileDto DTO с обновленными данными профиля
     * @return обновленный DTO профиля
     */
    UserProfileDto updateUserProfile(String keycloakId, UserProfileDto userProfileDto);
    
    /**
     * Удаляет профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     */
    void deleteUserProfile(String keycloakId);
}
