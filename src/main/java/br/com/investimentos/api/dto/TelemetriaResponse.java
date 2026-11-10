package br.com.investimentos.api.dto;

import java.util.List;

/** Envelope de resposta do GET /telemetria. */
public record TelemetriaResponse(
        List<ServicoTelemetriaDTO> servicos,
        PeriodoDTO periodo) {
}
