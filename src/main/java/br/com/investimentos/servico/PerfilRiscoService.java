package br.com.investimentos.servico;

import br.com.investimentos.dominio.AnalisePerfil;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.excecao.RecursoNaoEncontradoException;
import br.com.investimentos.persistencia.ClienteRepository;
import br.com.investimentos.persistencia.InvestimentoRepository;
import br.com.investimentos.persistencia.MovimentacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/** Carrega o comportamento financeiro do cliente e delega o calculo ao motor puro. */
@ApplicationScoped
public class PerfilRiscoService {

    static final int JANELA_MOVIMENTACOES_DIAS = 180;

    private final ClienteRepository clientes;
    private final InvestimentoRepository investimentos;
    private final MovimentacaoRepository movimentacoes;

    @Inject
    public PerfilRiscoService(ClienteRepository clientes,
                              InvestimentoRepository investimentos,
                              MovimentacaoRepository movimentacoes) {
        this.clientes = clientes;
        this.investimentos = investimentos;
        this.movimentacoes = movimentacoes;
    }

    public AnalisePerfil analisar(long clienteId) {
        clientes.buscar(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente %d não encontrado.".formatted(clienteId)));

        List<Investimento> posicoes = investimentos.listarPorCliente(clienteId);
        LocalDate inicioJanela = LocalDate.now(ZoneOffset.UTC).minusDays(JANELA_MOVIMENTACOES_DIAS);
        int quantidadeMovimentacoes = movimentacoes.contarDesde(clienteId, inicioJanela);

        return MotorPerfilRisco.analisar(clienteId, posicoes, quantidadeMovimentacoes);
    }
}
