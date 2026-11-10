package br.com.investimentos.api.dto;

import java.time.Instant;
import java.util.List;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String usuario,
        List<String> roles,
        Long clienteId,
        Instant expiraEm) {
}
