package com.tottalstore.sifen.common;

/**
 * Error de regla de negocio (dato duplicado, estado inválido para la operación, etc.),
 * distinto de un error de validación de formato de entrada.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
