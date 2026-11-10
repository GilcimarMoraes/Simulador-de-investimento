package br.com.investimentos.api;

import br.com.investimentos.api.dto.TelemetriaResponse;
import br.com.investimentos.dominio.TelemetriaResumo;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.servico.TelemetriaService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** Telemetria de volume e tempo de resposta por servico (restrita a ADMIN). */
@Path("/telemetria")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("ADMIN")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Telemetria", description = "Volume e latência por serviço")
public class TelemetriaResource {

    private static final int JANELA_PADRAO_DIAS = 30;
    private static final int JANELA_MAXIMA_DIAS = 366;

    @Inject
    TelemetriaService telemetria;

    @GET
    @Telemetrado("telemetria")
    @Operation(summary = "Retorna telemetria por serviço",
            description = "Quantidade de chamadas, tempo médio, p95, máximo e erros por serviço. "
                    + "Requer perfil ADMIN. Sem parâmetros, considera os últimos 30 dias.")
    @APIResponse(responseCode = "200", description = "Telemetria retornada")
    @APIResponse(responseCode = "403", description = "Requer perfil ADMIN")
    public TelemetriaResponse consultar(@QueryParam("inicio") String inicio,
                                        @QueryParam("fim") String fim) {
        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);
        LocalDate dataInicio = inicio == null || inicio.isBlank()
                ? hoje.minusDays(JANELA_PADRAO_DIAS)
                : data(inicio, "inicio");
        LocalDate dataFim = fim == null || fim.isBlank() ? hoje : data(fim, "fim");

        if (dataInicio.isAfter(dataFim)) {
            throw new RequisicaoInvalidaException("inicio deve ser anterior ou igual a fim.");
        }
        if (dataInicio.plusDays(JANELA_MAXIMA_DIAS).isBefore(dataFim)) {
            throw new RequisicaoInvalidaException(
                    "O intervalo máximo permitido é de %d dias.".formatted(JANELA_MAXIMA_DIAS));
        }
        TelemetriaResumo resumo = telemetria.consultar(dataInicio, dataFim);
        return Mapeador.telemetria(resumo);
    }

    private LocalDate data(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException(
                    "Parâmetro %s inválido: use o formato AAAA-MM-DD.".formatted(campo));
        }
    }
}
