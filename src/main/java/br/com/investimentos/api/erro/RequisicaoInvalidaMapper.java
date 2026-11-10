package br.com.investimentos.api.erro;

import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class RequisicaoInvalidaMapper implements ExceptionMapper<RequisicaoInvalidaException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RequisicaoInvalidaException excecao) {
        ErroResponse corpo = fabrica.criar(400, "requisicao-invalida", "Requisição inválida",
                excecao.getMessage(), List.of(), uriInfo.getPath());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
