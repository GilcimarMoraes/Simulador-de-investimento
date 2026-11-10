package br.com.investimentos.api;

import br.com.investimentos.api.dto.InvestimentoDTO;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.servico.ClienteService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/** Historico de investimentos do cliente (entrada do motor de perfil). */
@Path("/investimentos")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Investimentos", description = "Posições atuais do cliente")
public class InvestimentoResource {

    @Inject
    ClienteService clientes;

    @GET
    @Path("/{clienteId}")
    @Telemetrado("investimentos")
    @Operation(summary = "Lista o histórico de investimentos do cliente")
    @APIResponse(responseCode = "200", description = "Histórico retornado")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public List<InvestimentoDTO> listar(@PathParam("clienteId") long clienteId) {
        if (clienteId <= 0) {
            throw new RequisicaoInvalidaException("clienteId deve ser positivo.");
        }
        return clientes.investimentos(clienteId).stream().map(Mapeador::investimento).toList();
    }
}
