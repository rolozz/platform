package com.cor.userservice.controller;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.service.AdminService;
import com.cor.userservice.util.enam.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для административных функций управления пользователями.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Management", description = "API для административных функций управления пользователями")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Удалить пользователя по username", description = "Удаляет пользователя из Keycloak и базы данных", tags = {"Admin Management"})
    @DeleteMapping("/users/{username}")
    public ResponseEntity<Void> deleteUserByUsername(
            @Parameter(description = "Username пользователя для удаления") @PathVariable String username,
            @RequestHeader("X-Username") String adminUsername) {
        
        log.info("Request to delete user {} by admin {}", username, adminUsername);
        adminService.deleteUserByUsername(username, adminUsername);
        return ResponseEntity.noContent().build();
    }



    @Operation(summary = "Повысить роль пользователя", description = "Повышает роль пользователя до указанной", tags = {"Admin Management"})
    @PutMapping("/users/{username}/promote")
    public ResponseEntity<UserProfileDto> promoteUser(
            @Parameter(description = "Username пользователя") @PathVariable String username,
            @Parameter(description = "Новая роль") @RequestParam UserRole newRole,
            @RequestHeader("X-Username") String adminUsername) {
        
        log.info("Request to promote user {} to {} by admin {}", username, newRole, adminUsername);
        UserProfileDto result = adminService.promoteUser(username, newRole, adminUsername);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Понизить роль пользователя", description = "Понижает роль пользователя до указанной", tags = {"Admin Management"})
    @PutMapping("/users/{username}/demote")
    public ResponseEntity<UserProfileDto> demoteUser(
            @Parameter(description = "Username пользователя") @PathVariable String username,
            @Parameter(description = "Новая роль") @RequestParam UserRole newRole,
            @RequestHeader("X-Username") String adminUsername) {
        
        log.info("Request to demote user {} to {} by admin {}", username, newRole, adminUsername);
        UserProfileDto result = adminService.demoteUser(username, newRole, adminUsername);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Изменить роль пользователя", description = "Изменяет роль пользователя на указанную", tags = {"Admin Management"})
    @PutMapping("/users/{username}/role")
    public ResponseEntity<UserProfileDto> changeUserRole(
            @Parameter(description = "Username пользователя") @PathVariable String username,
            @Parameter(description = "Новая роль") @RequestParam UserRole newRole,
            @RequestHeader("X-Username") String adminUsername) {
        
        log.info("Request to change role of user {} to {} by admin {}", username, newRole, adminUsername);
        UserProfileDto result = adminService.changeUserRole(username, newRole, adminUsername);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Получить роль пользователя", description = "Возвращает текущую роль пользователя", tags = {"Admin Management"})
    @GetMapping("/users/{username}/role")
    public ResponseEntity<UserRole> getUserRole(
            @Parameter(description = "Username пользователя") @PathVariable String username) {
        
        log.info("Request to get role for user {}", username);
        UserRole role = adminService.getUserRole(username);
        return ResponseEntity.ok(role);
    }
}
