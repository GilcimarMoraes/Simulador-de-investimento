package br.com.investimentos.dominio;

import java.util.List;

/** Usuario da API (tabela {@code usuario}), com senha sempre em hash. */
public record Usuario(
        long id,
        String username,
        String senhaHash,
        List<String> roles,
        Long clienteId,
        boolean ativo) {
}
