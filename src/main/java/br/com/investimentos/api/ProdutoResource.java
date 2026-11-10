package br.com.investimentos.api;

import br.com.investimentos.api.dto.ProdutoDTO;
import br.com.investimentos.api.dto.ProdutoRecomendadoDTO;
import br.com.investimentos.dominio.PerfilRisco;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.servico.ProdutoService;
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

/** Catalogo de produtos e recomendacao por perfil. */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"USER", "ADMIN"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Produtos", description = "Catálogo parametrizado e recomendações")
public class ProdutoResource {

    @Inject
    ProdutoService produtos;

    @GET
    @Path("/produtos-recomendados/{perfil}")
    @Telemetrado("produtos-recomendados")
    @Operation(summary = "Lista produtos recomendados para o perfil",
            description = "Conservador recebe apenas risco baixo; moderado recebe baixo e médio; "
                    + "agressivo recebe todos. Ordenado por risco e rentabilidade.")
    @APIResponse(responseCode = "200", description = "Produtos recomendados")
    @APIResponse(responseCode = "400", description = "Perfil inválido")
    public List<ProdutoRecomendadoDTO> recomendados(@PathParam("perfil") String perfil) {
        try {
            return produtos.recomendar(PerfilRisco.de(perfil)).stream().map(Mapeador::recomendado).toList();
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException(e.getMessage());
        }
    }

    @GET
    @Path("/produtos")
    @Telemetrado("produtos")
    @Operation(summary = "Lista o catálogo de produtos ativos",
            description = "Fonte de verdade dos limites de valor, prazo e rentabilidade usados na simulação.")
    public List<ProdutoDTO> listar() {
        return produtos.listarAtivos().stream().map(Mapeador::produto).toList();
    }
}
