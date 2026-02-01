package com.cor.userservice.service;

import com.cor.userservice.dto.UserProfileDto;
import com.cor.userservice.util.enam.UserRole;

/**
 * Сервис для административных функций управления пользователями.
 */
public interface AdminService {
    
    /**
     * Удаляет пользователя по username из Keycloak и базы данных.
     * 
     * @param username username пользователя для удаления
     * @param adminUsername username администратора, выполняющего удаление
     * @throws SecurityException если администратор не имеет прав на удаление
     * @throws IllegalArgumentException если пользователь не найден
     */
    void deleteUserByUsername(String username, String adminUsername);
    
    /**
     * Повышает роль пользователя.
     * 
     * @param username username пользователя
     * @param newRole новая роль
     * @param adminUsername username администратора, выполняющего изменение
     * @return обновленный профиль пользователя
     * @throws SecurityException если администратор не имеет прав на изменение роли
     * @throws IllegalArgumentException если пользователь не найден или роль некорректна
     */
    UserProfileDto promoteUser(String username, UserRole newRole, String adminUsername);
    
    /**
     * Понижает роль пользователя.
     * 
     * @param username username пользователя
     * @param newRole новая роль
     * @param adminUsername username администратора, выполняющего изменение
     * @return обновленный профиль пользователя
     * @throws SecurityException если администратор не имеет прав на изменение роли
     * @throws IllegalArgumentException если пользователь не найден или роль некорректна
     */
    UserProfileDto demoteUser(String username, UserRole newRole, String adminUsername);
    
    /**
     * Изменяет роль пользователя.
     * 
     * @param username username пользователя
     * @param newRole новая роль
     * @param adminUsername username администратора, выполняющего изменение
     * @return обновленный профиль пользователя
     * @throws SecurityException если администратор не имеет прав на изменение роли
     * @throws IllegalArgumentException если пользователь не найден или роль некорректна
     */
    UserProfileDto changeUserRole(String username, UserRole newRole, String adminUsername);
    
    /**
     * Получает текущую роль пользователя из Keycloak.
     * 
     * @param username username пользователя
     * @return текущая роль пользователя
     * @throws IllegalArgumentException если пользователь не найден
     */
    UserRole getUserRole(String username);
}
