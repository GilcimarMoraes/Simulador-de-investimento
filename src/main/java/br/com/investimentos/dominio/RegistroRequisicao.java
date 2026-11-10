package br.com.investimentos.dominio;

import java.time.Instant;

/** Amostra de telemetria de uma chamada HTTP concluida. */
public record RegistroRequisicao(
        String servico,
        String metodo,
        String caminho,
        int status,
        long duracaoMs,
        String usuario,
        Instant dataHora) {
}
