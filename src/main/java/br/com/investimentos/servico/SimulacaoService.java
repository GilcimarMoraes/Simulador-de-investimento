package br.com.investimentos.servico;

import br.com.investimentos.dominio.AgregadoProdutoDia;
import br.com.investimentos.dominio.AnalisePerfil;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.ResultadoCalculo;
import br.com.investimentos.dominio.Risco;
import br.com.investimentos.dominio.Simulacao;
import br.com.investimentos.dominio.excecao.RequisicaoInvalidaException;
import br.com.investimentos.dominio.excecao.RegraDeNegocioException;
import br.com.investimentos.dominio.excecao.RecursoNaoEncontradoException;
import br.com.investimentos.persistencia.ClienteRepository;
import br.com.investimentos.persistencia.ProdutoRepository;
import br.com.investimentos.persistencia.SimulacaoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Orquestra a simulacao: valida o cliente e o tipo, filtra os produtos parametrizados no banco
 * pelos limites de valor/prazo, escolhe o mais aderente ao perfil e persiste o resultado.
 */
@ApplicationScoped
public class SimulacaoService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final ClienteRepository clientes;
    private final ProdutoRepository produtos;
    private final SimulacaoRepository simulacoes;
    private final PerfilRiscoService perfilService;

    @Inject
    public SimulacaoService(ClienteRepository clientes,
                            ProdutoRepository produtos,
                            SimulacaoRepository simulacoes,
                            PerfilRiscoService perfilService) {
        this.clientes = clientes;
        this.produtos = produtos;
        this.simulacoes = simulacoes;
        this.perfilService = perfilService;
    }

    @Transactional
    public SimulacaoRealizada simular(ComandoSimulacao comando) {
        String tipo = normalizarTipo(comando.tipoProduto());
        validarTipo(tipo);
        clientes.buscar(comando.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente %d não encontrado.".formatted(comando.clienteId())));

        AnalisePerfil analise = perfilService.analisar(comando.clienteId());
        List<Produto> candidatos = produtos.listarAtivosPorTipo(tipo).stream()
                .filter(produto -> produto.aceita(comando.valor(), comando.prazoMeses()))
                .toList();

        if (candidatos.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Nenhum produto do tipo %s atende aos parâmetros informados (valor %s, prazo %d meses). "
                            .formatted(tipo, moeda(comando.valor()), comando.prazoMeses())
                            + "Consulte GET /produtos para verificar os limites de cada produto.");
        }

        Produto escolhido = candidatos.stream()
                .min(melhorProduto(analise, comando.valor(), comando.prazoMeses()))
                .orElseThrow();

        ResultadoCalculo calculo = CalculadoraInvestimento.calcular(
                escolhido, comando.valor(), comando.prazoMeses());

        Instant agora = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Simulacao simulacao = new Simulacao(
                0L,
                comando.clienteId(),
                escolhido.id(),
                escolhido.nome(),
                escolhido.tipo(),
                comando.valor(),
                calculo.valorFinal(),
                calculo.valorFinalLiquido(),
                calculo.imposto(),
                calculo.rentabilidadeEfetiva(),
                comando.prazoMeses(),
                analise.perfil(),
                agora);

        long id = simulacoes.inserir(simulacao);
        Simulacao persistida = new Simulacao(
                id, simulacao.clienteId(), simulacao.produtoId(), simulacao.produtoNome(),
                simulacao.tipoProduto(), simulacao.valorInvestido(), simulacao.valorFinal(),
                simulacao.valorFinalLiquido(), simulacao.impostoRendimento(),
                simulacao.rentabilidadeEfetiva(), simulacao.prazoMeses(), simulacao.perfilCliente(),
                simulacao.dataSimulacao());

        return new SimulacaoRealizada(persistida, escolhido, calculo, alertas(analise, escolhido));
    }

    /** Lista o historico; quando {@code clienteId} e informado, valida a existencia do cliente. */
    public List<Simulacao> listar(Long clienteId) {
        if (clienteId != null) {
            clientes.buscar(clienteId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Cliente %d não encontrado.".formatted(clienteId)));
        }
        return simulacoes.listar(clienteId);
    }

    /** Agregado diario por produto no intervalo inclusivo [inicio, fim]. */
    public List<AgregadoProdutoDia> porProdutoDia(LocalDate inicio, LocalDate fim) {
        return simulacoes.porProdutoDia(inicio, fim.plusDays(1));
    }

    /**
     * Ordena do candidato mais adequado para o menos adequado: maior aderencia ao perfil,
     * depois maior rentabilidade liquida para o prazo pedido e, por fim, id (desempate estavel).
     */
    private Comparator<Produto> melhorProduto(AnalisePerfil analise, BigDecimal valor, int prazoMeses) {
        return Comparator
                .comparingInt((Produto p) -> analise.perfil().aderencia(p.risco())).reversed()
                .thenComparing(p -> CalculadoraInvestimento
                        .calcular(p, valor, prazoMeses).rentabilidadeLiquidaEfetiva(), Comparator.reverseOrder())
                .thenComparingLong(Produto::id);
    }

    private List<String> alertas(AnalisePerfil analise, Produto produto) {
        List<String> alertas = new ArrayList<>();
        if (!analise.perfil().aceita(produto.risco())) {
            alertas.add("O produto %s tem risco %s, acima da tolerancia do perfil %s."
                    .formatted(produto.nome(), produto.risco().descricao(), analise.perfil().descricao()));
        }
        if (produto.carenciaMeses() > 0) {
            alertas.add("Produto com carência de %d meses; resgate antecipado não é permitido."
                    .formatted(produto.carenciaMeses()));
        }
        return List.copyOf(alertas);
    }

    private void validarTipo(String tipo) {
        List<String> disponiveis = produtos.tiposDisponiveis();
        if (!disponiveis.contains(tipo)) {
            throw new RequisicaoInvalidaException(
                    "Tipo de produto '%s' inválido. Tipos disponíveis: %s."
                            .formatted(tipo, String.join(", ", disponiveis)));
        }
    }

    static String normalizarTipo(String tipo) {
        String normalizado = Risco.Texto.normalizar(tipo);
        return switch (normalizado) {
            case "TESOURO DIRETO" -> "TESOURO";
            default -> normalizado;
        };
    }

    private String moeda(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }
}
