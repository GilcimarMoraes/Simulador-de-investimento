package br.com.investimentos.api;

import br.com.investimentos.api.dto.LoginRequest;
import br.com.investimentos.api.dto.TokenResponse;
import br.com.investimentos.api.dto.UsuarioAtualResponse;
import br.com.investimentos.persistencia.UsuarioRepository;
import br.com.investimentos.servico.AutenticacaoService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;

/** Emissao e leitura do token JWT usado pelos demais endpoints. */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Autenticação", description = "Obtenção e inspeção do token JWT")
public class AuthResource {

    @Inject
    AutenticacaoService autenticacao;

    @Inject
    JsonWebToken jwt;

    @Inject
    UsuarioRepository usuarios;

    @POST
    @Path("/token")
    @PermitAll
    @Telemetrado("autenticacao")
    @Operation(summary = "Obtém um token JWT",
            description = "Autentica usuário e senha e devolve um JWT RS256 com validade de 1 hora.")
    @APIResponse(responseCode = "200", description = "Token emitido")
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "401", description = "Credenciais inválidas")
    public TokenResponse token(@Valid LoginRequest requisicao) {
        return autenticacao.autenticar(requisicao.username(), requisicao.password());
    }

    @GET
    @Path("/me")
    @RolesAllowed({"USER", "ADMIN"})
    @Telemetrado("usuario-atual")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Retorna os dados do token atual")
    public UsuarioAtualResponse eu() {
        Long clienteId = usuarios.buscarPorUsername(jwt.getName())
                .map(usuario -> usuario.clienteId())
                .orElse(null);
        return new UsuarioAtualResponse(
                jwt.getName(),
                jwt.getGroups().stream().sorted().toList(),
                clienteId,
                Instant.ofEpochSecond(jwt.getExpirationTime()));
    }
}
