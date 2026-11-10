package br.com.investimentos.api.erro;

import br.com.investimentos.dominio.excecao.RegraDeNegocioException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class RegraDeNegocioMapper implements ExceptionMapper<RegraDeNegocioException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RegraDeNegocioException excecao) {
        ErroResponse corpo = fabrica.criar(422, "regra-de-negocio", "Regra de negócio não atendida",
                excecao.getMessage(), List.of(), uriInfo.getPath());
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
