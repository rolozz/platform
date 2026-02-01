package com.cor.userservice.util.exception;

import lombok.Getter;

/**
 * Исключение, возникающее при попытке создать пользователя, который уже существует в системе
 */
@Getter
public class UserAlreadyExistsException extends RuntimeException {
    private final String fieldName;
    private final String value;

    public UserAlreadyExistsException(String fieldName, String value) {
        super(String.format("Пользователь с %s '%s' уже существует", fieldName, value));
        this.fieldName = fieldName;
        this.value = value;
    }
}
