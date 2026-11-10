package br.com.investimentos.api;

import br.com.investimentos.api.dto.PerfilRiscoResponse;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.servico.PerfilRiscoService;
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

/** Perfil de risco calculado dinamicamente. */
@Path("/perfil-risco")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Perfil de risco", description = "Motor de recomendação baseado em comportamento")
public class PerfilResource {

    @Inject
    PerfilRiscoService perfis;

    @GET
    @Path("/{clienteId}")
    @Telemetrado("perfil-risco")
    @Operation(summary = "Calcula o perfil de risco do cliente",
            description = "Pontua volume investido, frequência de movimentações (180 dias), "
                    + "preferência por liquidez e apetite por retorno. Retorna também os fatores "
                    + "que compõem a pontuação.")
    @APIResponse(responseCode = "200", description = "Perfil calculado")
    @APIResponse(responseCode = "404", description = "Cliente não encontrado")
    public PerfilRiscoResponse perfil(@PathParam("clienteId") long clienteId) {
        if (clienteId <= 0) {
            throw new RequisicaoInvalidaException("clienteId deve ser positivo.");
        }
        return Mapeador.perfil(perfis.analisar(clienteId));
    }
}
