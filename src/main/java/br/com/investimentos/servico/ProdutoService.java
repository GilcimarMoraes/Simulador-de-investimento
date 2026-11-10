package br.com.investimentos.servico;

import br.com.investimentos.dominio.PerfilRisco;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.persistencia.ProdutoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;

/** Catalogo e recomendacao de produtos por perfil. */
@ApplicationScoped
public class ProdutoService {

    private final ProdutoRepository produtos;

    @Inject
    public ProdutoService(ProdutoRepository produtos) {
        this.produtos = produtos;
    }

    public List<Produto> listarAtivos() {
        return produtos.listarAtivos();
    }

    public List<String> tiposDisponiveis() {
        return produtos.tiposDisponiveis();
    }

    /**
     * Produtos aderentes ao perfil, ordenados por risco crescente e rentabilidade decrescente.
     * Conservador recebe apenas risco baixo; moderado recebe baixo/medio; agressivo recebe todos.
     */
    public List<Produto> recomendar(PerfilRisco perfil) {
        return produtos.listarAtivos().stream()
                .filter(p -> perfil.aceita(p.risco()))
                .sorted(Comparator
                        .comparingInt((Produto p) -> p.risco().nivel())
                        .thenComparing(Produto::rentabilidadeAnual, Comparator.reverseOrder())
                        .thenComparingLong(Produto::id))
                .toList();
    }
}
