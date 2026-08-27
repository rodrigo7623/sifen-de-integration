package com.tottalstore.sifen.common;

import java.time.Instant;
import java.util.List;

public record ApiError(Instant timestamp, int status, String error, String message, List<String> detalles) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, List.of());
    }

    public static ApiError of(int status, String error, String message, List<String> detalles) {
        return new ApiError(Instant.now(), status, error, message, detalles);
    }
}
