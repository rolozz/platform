package com.cor.apigateway.util;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Утилита для работы с JWT токенами.
 * Предоставляет методы для валидации токенов и извлечения данных из них.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final ReactiveCircuitBreakerFactory<?, ?> reactiveCircuitBreakerFactory;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    private RSAPublicKey publicKey;
    private long lastKeyRefresh = 0;
    private static final long KEY_REFRESH_INTERVAL = 3600000; // 1 час

    /**
     * Валидирует JWT токен.
     * Проверяет подпись, срок действия и издателя токена.
     * 
     * @param token JWT токен для валидации
     * @return true если токен валиден, иначе false
     */
    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            
            // Проверка подписи
            if (!verifySignature(signedJWT)) {
                log.error("Неверная подпись JWT токена");
                return false;
            }

            // Проверка срока действия
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                log.error("Срок действия JWT токена истек");
                return false;
            }

            // Проверка издателя
            String issuer = claimsSet.getIssuer();
            String expectedIssuer = authServerUrl + "/realms/" + realm;
            if (issuer == null || !issuer.equals(expectedIssuer)) {
                log.error("Неверный издатель JWT токена: {}", issuer);
                return false;
            }

            return true;
        } catch (ParseException e) {
            log.error("Ошибка парсинга JWT токена", e);
            return false;
        }
    }

    /**
     * Извлекает ID пользователя из JWT токена.
     * 
     * @param token JWT токен
     * @return ID пользователя или null в случае ошибки
     */
    public String getUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return claimsSet.getSubject();
        } catch (ParseException e) {
            log.error("Ошибка извлечения ID пользователя из JWT", e);
            return null;
        }
    }

    /**
     * Извлекает имя пользователя из JWT токена.
     * 
     * @param token JWT токен
     * @return имя пользователя или null в случае ошибки
     */
    public String getUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return claimsSet.getStringClaim("preferred_username");
        } catch (ParseException e) {
            log.error("Ошибка извлечения имени пользователя из JWT", e);
            return null;
        }
    }

    /**
     * Извлекает роли пользователя из JWT токена.
     * 
     * @param token JWT токен
     * @return список ролей или null в случае ошибки
     */
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            return (List<String>) claimsSet.getClaim("roles");
        } catch (ParseException e) {
            log.error("Ошибка извлечения ролей из JWT", e);
            return Collections.emptyList();
        }
    }

    /**
     * Проверяет подпись JWT токена с использованием публичного ключа.
     * 
     * @param signedJWT подписанный JWT токен
     * @return true если подпись верна, иначе false
     */
    private boolean verifySignature(SignedJWT signedJWT) {
        try {
            if (publicKey == null || System.currentTimeMillis() - lastKeyRefresh > KEY_REFRESH_INTERVAL) {
                refreshPublicKey();
            }

            if (publicKey == null) {
                log.error("Публичный ключ недоступен");
                return false;
            }

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            return signedJWT.verify(verifier);
        } catch (JOSEException e) {
            log.error("Ошибка проверки подписи JWT", e);
            return false;
        }
    }

    /**
     * Обновляет публичный ключ из Keycloak JWKS эндпоинта.
     */
    private void refreshPublicKey() {
        try {
            var circuitBreaker = reactiveCircuitBreakerFactory.create("keycloakService");

            Mono<RSAPublicKey> fetchMono = Mono.fromCallable(() -> {
                String jwksUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
                JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));

                List<JWK> keys = jwkSet.getKeys();
                if (keys.isEmpty()) {
                    return null;
                }

                JWK jwk = keys.get(0);
                if (!(jwk instanceof RSAKey rsaKey)) {
                    return null;
                }

                return rsaKey.toRSAPublicKey();
            })
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(Mono::justOrEmpty);

            RSAPublicKey fetchedKey = circuitBreaker
                    .run(fetchMono, throwable -> {
                        log.error("Keycloak недоступен, circuit breaker сработал при обновлении публичного ключа", throwable);
                        return Mono.empty();
                    })
                    .block();

            if (fetchedKey != null) {
                this.publicKey = fetchedKey;
                this.lastKeyRefresh = System.currentTimeMillis();
                log.info("Публичный ключ успешно обновлен");
            }
        } catch (Exception e) {
            log.error("Ошибка обновления публичного ключа из Keycloak", e);
        }
    }
}
