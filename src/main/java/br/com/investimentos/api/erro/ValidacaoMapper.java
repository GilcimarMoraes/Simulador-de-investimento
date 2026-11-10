package br.com.investimentos.api.erro;

import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Converte violacoes de Bean Validation em Problem Details 400 com a lista de campos. */
@Provider
public class ValidacaoMapper implements ExceptionMapper<ConstraintViolationException> {

    @Inject
    ErroFactory fabrica;

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException excecao) {
        List<CampoErro> erros = campos(excecao.getConstraintViolations());
        ErroResponse corpo = fabrica.criar(400, "validacao", "Dados de entrada inválidos",
                "Um ou mais campos não passaram na validação.", erros, uriInfo.getPath());
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(corpo)
                .build();
    }

    static List<CampoErro> campos(Set<? extends ConstraintViolation<?>> violacoes) {
        if (violacoes == null || violacoes.isEmpty()) {
            return List.of();
        }
        return violacoes.stream()
                .map(v -> new CampoErro(campo(v.getPropertyPath().toString()), v.getMessage()))
                .sorted(Comparator.comparing(CampoErro::campo))
                .toList();
    }

    private static String campo(String caminho) {
        if (caminho == null || caminho.isBlank()) {
            return "requisicao";
        }
        int ultimoPonto = caminho.lastIndexOf('.');
        String folha = ultimoPonto >= 0 ? caminho.substring(ultimoPonto + 1) : caminho;
        return folha.contains("<") ? "requisicao" : folha;
    }
}
