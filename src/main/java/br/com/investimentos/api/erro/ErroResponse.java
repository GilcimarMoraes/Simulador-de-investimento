package br.com.investimentos.api.erro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Corpo de erro no formato Problem Details (RFC 9457), acrescido de correlation id e lista de
 * erros de campo. Nenhuma mensagem interna ou stack trace e exposta ao cliente.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErroResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        Instant timestamp,
        String requestId,
        List<CampoErro> erros) {
}
