package com.cor.userservice.util.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Класс для представления ошибок в API.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * Временная метка возникновения ошибки.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * HTTP статус код.
     */
    private int status;

    /**
     * Название HTTP статуса.
     */
    private String error;

    /**
     * Сообщение об ошибке.
     */
    private String message;

    /**
     * Путь, по которому произошла ошибка.
     */
    private String path;
}
