package com.cor.userservice.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация для работы с Keycloak (сервер аутентификации).
 * Содержит настройки подключения и создает необходимые бины для взаимодействия с Keycloak.
 */
@Slf4j
@Getter
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KeycloakConfig {

    /**
     * Название realm в Keycloak
     */
    private final String realm;

    /**
     * Базовый URL сервера Keycloak
     */
    private final String authServerUrl;

    /**
     * ID клиентского приложения в Keycloak
     */
    private final String clientId;

    /**
     * Секрет клиентского приложения
     */
    private final String clientSecret;

    /**
     * Логин администратора Keycloak
     */
    private final String adminUsername;

    /**
     * Пароль администратора Keycloak
     */
    private final String adminPassword;

    /**
     * ID клиента для административных операций
     */
    private final String adminClientId;

    /**
     * Realm для административных операций
     */
    private final String adminRealm;

    /**
     * Конструктор конфигурации Keycloak.
     *
     * @param realm         название realm
     * @param authServerUrl URL сервера аутентификации
     * @param clientId      ID клиента
     * @param clientSecret  секрет клиента
     * @param adminUsername логин администратора
     * @param adminPassword пароль администратора
     * @param adminClientId ID клиента для админки
     * @param adminRealm    realm для админки
     */
    public KeycloakConfig(
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.auth-server-url}") String authServerUrl,
            @Value("${keycloak.client-id}") String clientId,
            @Value("${keycloak.client-secret}") String clientSecret,
            @Value("${keycloak.admin.username}") String adminUsername,
            @Value("${keycloak.admin.password}") String adminPassword,
            @Value("${keycloak.admin.client-id}") String adminClientId,
            @Value("${keycloak.admin.realm}") String adminRealm) {
        this.realm = realm;
        this.authServerUrl = authServerUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.adminClientId = adminClientId;
        this.adminRealm = adminRealm;
    }

    /**
     * Создает WebClient для взаимодействия с Keycloak API.
     *
     * @return настроенный WebClient для запросов к Keycloak
     */
    @Bean
    public WebClient keycloakWebClient() {
        log.info("Создание WebClient для Keycloak с базовым URL: {}/realms/{}", authServerUrl, realm);
        return WebClient.builder()
                .baseUrl(authServerUrl + "/realms/" + realm + "/protocol/openid-connect")
                .build();
    }

    /**
     * Создает административный клиент Keycloak.
     *
     * @return экземпляр Keycloak для административных операций
     */
    @Bean
    public Keycloak keycloak() {
        log.info("Создание Keycloak Admin Client для реалма: {}", adminRealm);
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(adminRealm)
                .username(adminUsername)
                .password(adminPassword)
                .clientId(adminClientId)
                .build();
        log.info("Keycloak Admin Client успешно создан");
        return keycloak;
    }
}
