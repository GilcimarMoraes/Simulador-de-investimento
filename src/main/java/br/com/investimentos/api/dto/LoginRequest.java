package br.com.investimentos.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username é obrigatório")
        @Size(max = 80, message = "username deve ter no máximo 80 caracteres")
        String username,

        @NotBlank(message = "password é obrigatório")
        @Size(max = 200, message = "password deve ter no máximo 200 caracteres")
        String password) {
}
