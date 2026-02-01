package com.cor.userservice.util.enam;

/**
 * Перечисление ролей пользователей в системе.
 */
public enum UserRole {
    USER("USER", "Базовая роль пользователя"),
    ADMIN("ADMIN", "Администратор системы"),
    OWNER("OWNER", "Владелец - максимальные привилегии");

    private final String roleName;
    private final String description;

    UserRole(String roleName, String description) {
        this.roleName = roleName;
        this.description = description;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Проверяет, может ли текущая роль управлять указанной ролью.
     * 
     * @param currentRole роль текущего пользователя
     * @param targetRole роль целевого пользователя
     * @return true если текущая роль может управлять целевой
     */
    public static boolean canManageRole(UserRole currentRole, UserRole targetRole) {
        if (currentRole == null || targetRole == null) {
            return false;
        }

        if (currentRole == OWNER) {
            return true;
        }

        if (currentRole == ADMIN) {
            return targetRole == USER;
        }

        return false;
    }

    /**
     * Получает роль по строковому имени.
     * 
     * @param roleName имя роли
     * @return UserRole или null если не найдена
     */
    public static UserRole fromString(String roleName) {
        for (UserRole role : values()) {
            if (role.roleName.equals(roleName)) {
                return role;
            }
        }
        return null;
    }
}
