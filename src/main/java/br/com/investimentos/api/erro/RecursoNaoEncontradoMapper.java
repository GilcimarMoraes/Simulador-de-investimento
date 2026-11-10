package br.com.investimentos.api.erro;

import br.com.investimentos.dominio.excecao.RecursoNaoEncontradoException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class RecursoNaoEncontradoMapper implements ExceptionMapper<RecursoNaoEncontradoException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RecursoNaoEncontradoException excecao) {
        ErroResponse corpo = fabrica.criar(404, "nao-encontrado", "Recurso não encontrado",
                excecao.getMessage(), List.of(), uriInfo.getPath());
        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
