package com.cor.userservice.controller;

import com.cor.userservice.dto.UserProfileKeycloakDto;
import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для работы с профилями пользователей.
 */
@Tag(name = "User Profile API", description = "API для работы с профилями пользователей")
@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * Получает профиль пользователя по его идентификатору.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @return DTO профиля пользователя
     */
    @Operation(summary = "Получить профиль пользователя по ID", tags = {"User Profile API"})
    @GetMapping("/get")
    public ResponseEntity<UserProfileDto> getUserProfile(@RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(userProfileService.getUserProfile(keycloakId));
    }

    /**
     * Получает список всех профилей пользователей.
     *
     * @return список DTO профилей пользователей
     */
    @Operation(summary = "Получить все профили пользователей", tags = {"User Profile API"})
    @GetMapping("/all")
    public ResponseEntity<Page<UserProfileDto>> getAllUserProfiles(
            @PageableDefault(
                    size = 20,
                    page = 1,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        Page<UserProfileDto> result = userProfileService.getAllUserProfiles(pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Создает новый профиль пользователя.
     *
     * @param userProfileCreateDto DTO с данными нового профиля
     * @return DTO созданного профиля
     */
    @Operation(summary = "Создать новый профиль пользователя", tags = {"User Profile API"})
    @PostMapping("/create")
    public ResponseEntity<UserProfileDto> createUserProfile(
            @Valid @RequestBody UserProfileKeycloakDto userProfileCreateDto) {
        return new ResponseEntity<>(
                userProfileService.createUserProfile(userProfileCreateDto),
                HttpStatus.CREATED
        );
    }

    /**
     * Обновляет существующий профиль пользователя.
     *
     * @param keycloakId    идентификатор пользователя в Keycloak
     * @param userProfileDto DTO с обновленными данными профиля
     * @return обновленный DTO профиля
     */
    @Operation(summary = "Обновить профиль пользователя", tags = {"User Profile API"})
    @PutMapping("/update")
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @RequestHeader("X-User-Id") String keycloakId,
            @Valid @RequestBody UserProfileKeycloakDto userProfileDto) {
        return ResponseEntity.ok(userProfileService.updateUserProfile(keycloakId, userProfileDto));
    }

    /**
     * Удаляет профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @return ответ с кодом 204 (No Content)
     */
    @Operation(summary = "Удалить профиль пользователя", tags = {"User Profile API"})
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUserProfile(@RequestHeader("X-User-Id") String keycloakId) {
        userProfileService.deleteUserProfile(keycloakId);
        return ResponseEntity.noContent().build();
    }
}
