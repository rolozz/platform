package com.cor.userservice.util.exception;

/**
 * Исключение, выбрасываемое при отсутствии запрашиваемого ресурса.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Создает новое исключение с указанным сообщением.
     *
     * @param message сообщение об ошибке
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Создает новое исключение с сообщением и причиной.
     *
     * @param message сообщение об ошибке
     * @param cause причина исключения
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
