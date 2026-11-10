package br.com.investimentos.api.erro;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

/** JSON malformado ou corpo ilegivel: 400 sem expor detalhes do parser. */
@Provider
public class CorpoInvalidoMapper implements ExceptionMapper<BadRequestException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(BadRequestException excecao) {
        ErroResponse corpo = fabrica.criar(400, "corpo-invalido", "Corpo da requisição inválido",
                "O corpo enviado não é um JSON válido ou não corresponde ao contrato esperado.",
                List.of(), uriInfo.getPath());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
