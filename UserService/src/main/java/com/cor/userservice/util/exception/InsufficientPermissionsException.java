package com.cor.userservice.util.exception;

/**
 * Исключение, возникающее при недостаточных правах доступа.
 * Заменяет SecurityException для бизнес-логики приложения.
 */
public class InsufficientPermissionsException extends RuntimeException {

    public InsufficientPermissionsException(String message) {
        super(message);
    }

    public InsufficientPermissionsException(String message, Throwable cause) {
        super(message, cause);
    }
}
