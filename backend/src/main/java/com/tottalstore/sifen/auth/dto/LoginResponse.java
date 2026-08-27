package com.tottalstore.sifen.auth.dto;

public record LoginResponse(String token, String nombre, String email, String rol) {
}
