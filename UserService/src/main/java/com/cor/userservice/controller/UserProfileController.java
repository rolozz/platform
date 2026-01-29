package com.cor.userservice.controller;

import com.cor.userservice.dto.UserProfileCreateDto;
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

import java.util.List;

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
    @Operation(summary = "Получить профиль пользователя по ID")
    @GetMapping("/get")
    public ResponseEntity<UserProfileDto> getUserProfile(@RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(userProfileService.getUserProfile(keycloakId));
    }

    /**
     * Получает список всех профилей пользователей.
     *
     * @return список DTO профилей пользователей
     */
    @Operation(summary = "Получить все профили пользователей")
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
    @Operation(summary = "Создать новый профиль пользователя")
    @PostMapping("/create")
    public ResponseEntity<UserProfileDto> createUserProfile(
            @Valid @RequestBody UserProfileCreateDto userProfileCreateDto) {
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
    @Operation(summary = "Обновить профиль пользователя")
    @PutMapping("/update")
    public ResponseEntity<UserProfileDto> updateUserProfile(
            @RequestHeader("X-User-Id") String keycloakId,
            @Valid @RequestBody UserProfileDto userProfileDto) {
        return ResponseEntity.ok(userProfileService.updateUserProfile(keycloakId, userProfileDto));
    }

    /**
     * Удаляет профиль пользователя.
     *
     * @param keycloakId идентификатор пользователя в Keycloak
     * @return ответ с кодом 204 (No Content)
     */
    @Operation(summary = "Удалить профиль пользователя")
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUserProfile(@RequestHeader("X-User-Id") String keycloakId) {
        userProfileService.deleteUserProfile(keycloakId);
        return ResponseEntity.noContent().build();
    }
}
