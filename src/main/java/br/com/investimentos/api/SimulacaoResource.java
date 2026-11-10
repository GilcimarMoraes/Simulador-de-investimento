package br.com.investimentos.api;

import br.com.investimentos.api.dto.RequisicaoSimulacao;
import br.com.investimentos.api.dto.SimulacaoHistoricoDTO;
import br.com.investimentos.api.dto.SimulacaoPorProdutoDiaDTO;
import br.com.investimentos.api.dto.SimulacaoResponse;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.servico.ComandoSimulacao;
import br.com.investimentos.servico.SimulacaoRealizada;
import br.com.investimentos.servico.SimulacaoService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.util.List;

/** Simulacao de investimentos, historico e agregado diario por produto. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Simulações", description = "Simulação, histórico e agregados")
public class SimulacaoResource {

    private static final int JANELA_MAXIMA_DIAS = 366;
    private static final int JANELA_PADRAO_DIAS = 30;

    @Inject
    SimulacaoService simulacoes;

    @POST
    @Path("/simular-investimento")
    @Telemetrado("simular-investimento")
    @Operation(summary = "Simula um investimento",
            description = "Valida o cliente, filtra os produtos parametrizados no banco por tipo, "
                    + "valor e prazo, escolhe o mais aderente ao perfil de risco e persiste o resultado.")
    @APIResponse(responseCode = "200", description = "Simulação realizada")
    @APIResponse(responseCode = "400", description = "Dados de entrada inválidos")
    @APIResponse(responseCode = "401", description = "Não autenticado")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    @APIResponse(responseCode = "422", description = "Nenhum produto atende aos parâmetros")
    public SimulacaoResponse simular(@Valid RequisicaoSimulacao requisicao) {
        SimulacaoRealizada realizada = simulacoes.simular(new ComandoSimulacao(
                requisicao.clienteId(),
                requisicao.valor(),
                requisicao.prazoMeses(),
                requisicao.tipoProduto()));
        return Mapeador.simulacao(realizada);
    }

    @GET
    @Path("/simulacoes")
    @Telemetrado("simulacoes")
    @Operation(summary = "Lista as simulações realizadas",
            description = "Retorna todas as simulações, da mais recente para a mais antiga. "
                    + "Use o filtro opcional clienteId.")
    public List<SimulacaoHistoricoDTO> listar(@QueryParam("clienteId") Long clienteId) {
        if (clienteId != null && clienteId <= 0) {
            throw new RequisicaoInvalidaException("clienteId deve ser positivo.");
        }
        return simulacoes.listar(clienteId).stream().map(Mapeador::historico).toList();
    }

    @GET
    @Path("/simulacoes/por-produto-dia")
    @Telemetrado("simulacoes-por-produto-dia")
    @Operation(summary = "Valores simulados por produto e dia",
            description = "Agrega quantidade de simulações e média do valor final por produto e dia. "
                    + "Sem parâmetros, considera os últimos 30 dias.")
    public List<SimulacaoPorProdutoDiaDTO> porProdutoDia(@QueryParam("inicio") String inicio,
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
        return simulacoes.porProdutoDia(dataInicio, dataFim).stream().map(Mapeador::agregado).toList();
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
