package com.cor.userservice.util.exception;

/**
 * Исключение, возникающее при проблемах с Keycloak
 */
public class KeycloakOperationException extends RuntimeException {
    public KeycloakOperationException(String message) {
        super(message);
    }

    public KeycloakOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
