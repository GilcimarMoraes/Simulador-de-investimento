package br.com.investimentos.api.erro;

import br.com.investimentos.api.RequestContexto;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;

/** Monta o corpo Problem Details com correlation id e caminho da requisicao. */
@RequestScoped
public class ErroFactory {

    private static final String BASE_TIPO = "https://painel-investimentos.local/erros/";

    @Inject
    RequestContexto contexto;

    public ErroResponse criar(int status, String slug, String titulo, String detalhe,
                              List<CampoErro> erros, String instancia) {
        return new ErroResponse(
                BASE_TIPO + slug,
                titulo,
                status,
                detalhe,
                instancia,
                Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
                contexto.id(),
                erros == null ? List.of() : erros);
    }
}
