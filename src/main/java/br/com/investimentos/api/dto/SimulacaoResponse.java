package br.com.investimentos.api.dto;

import java.time.Instant;
import java.util.List;

/** Envelope de resposta do POST /simular-investimento. */
public record SimulacaoResponse(
        ProdutoValidadoDTO produtoValidado,
        ResultadoSimulacaoDTO resultadoSimulacao,
        Instant dataSimulacao,
        long simulacaoId,
        long clienteId,
        String perfilCliente,
        List<String> alertas) {
}
