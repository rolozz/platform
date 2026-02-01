package com.cor.userservice.service;

import com.cor.userservice.dto.UserProfileKeycloakDto;
import com.cor.userservice.dto.UserProfileDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * @param userProfileCreateDtoDto DTO с данными нового профиля
     * @return DTO созданного профиля
     */
    UserProfileDto createUserProfile(UserProfileKeycloakDto userProfileCreateDtoDto);
    
    /**
     * Обновляет существующий профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @param userProfileDto DTO с обновленными данными профиля
     * @return обновленный DTO профиля
     */
    UserProfileDto updateUserProfile(String keycloakId, UserProfileKeycloakDto userProfileDto);
    
    /**
     * Удаляет профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     */
    void deleteUserProfile(String keycloakId);
}
