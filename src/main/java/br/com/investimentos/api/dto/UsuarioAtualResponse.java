package br.com.investimentos.api.dto;

import java.time.Instant;
import java.util.List;

public record UsuarioAtualResponse(
        String usuario,
        List<String> roles,
        Long clienteId,
        Instant expiraEm) {
}
