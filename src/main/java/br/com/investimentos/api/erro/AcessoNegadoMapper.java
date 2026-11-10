package br.com.investimentos.api.erro;

import io.quarkus.security.ForbiddenException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** 403 para usuarios autenticados sem o papel exigido (ex.: /telemetria exige ADMIN). */
@Provider
@Priority(1)
public class AcessoNegadoMapper implements ExceptionMapper<ForbiddenException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ForbiddenException excecao) {
        ErroResponse corpo = fabrica.criar(403, "acesso-negado", "Acesso negado",
                "O usuário autenticado não possui permissão para acessar este recurso.",
                List.of(), uriInfo.getPath());
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
