package br.com.investimentos.api.erro;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * O Quarkus REST envolve violacoes de corpo em {@code ResteasyReactiveViolationException};
 * este mapper garante o mesmo envelope Problem Details usado pelas demais validacoes.
 */
@Provider
public class ValidacaoRestMapper implements ExceptionMapper<ResteasyReactiveViolationException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResteasyReactiveViolationException excecao) {
        ErroResponse corpo = fabrica.criar(400, "validacao", "Dados de entrada inválidos",
                "Um ou mais campos não passaram na validação.",
                ValidacaoMapper.campos(excecao.getConstraintViolations()), uriInfo.getPath());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
