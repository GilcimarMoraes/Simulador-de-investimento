package br.com.investimentos.api.erro;

import br.com.investimentos.api.RequestContexto;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Rede de seguranca para excecoes nao tratadas: loga o stack trace completo no servidor e devolve
 * apenas uma mensagem generica com o requestId para correlacao, sem vazar detalhes internos.
 */
@Provider
public class ErroInternoMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ErroInternoMapper.class);

    @Inject
    ErroFactory fabrica;

    @Inject
    RequestContexto contexto;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable excecao) {
        LOG.errorf(excecao, "Erro nao tratado na requisicao %s", contexto.id());
        ErroResponse corpo = fabrica.criar(500, "erro-interno", "Erro interno",
                "Erro inesperado ao processar a requisição. Informe o requestId ao suporte.",
                List.of(), uriInfo.getPath());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }
}
