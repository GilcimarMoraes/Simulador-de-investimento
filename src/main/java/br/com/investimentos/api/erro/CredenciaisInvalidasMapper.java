package br.com.investimentos.api.erro;

import br.com.investimentos.dominio.excecao.CredenciaisInvalidasException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class CredenciaisInvalidasMapper implements ExceptionMapper<CredenciaisInvalidasException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(CredenciaisInvalidasException excecao) {
        ErroResponse corpo = fabrica.criar(401, "credenciais-invalidas", "Credenciais inválidas",
                excecao.getMessage(), List.of(), uriInfo.getPath());
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .header("WWW-Authenticate", "Bearer realm=\"painel-investimentos\"")
                .entity(corpo)
                .build();
    }
}
