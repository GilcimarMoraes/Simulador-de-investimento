package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.Liquidez;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.Risco;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Acesso a tabela parametrizada de produtos. Os limites de valor/prazo consultados aqui sao a
 * fonte de verdade da validacao da simulacao (nao ha regra duplicada em codigo).
 */
@ApplicationScoped
public class ProdutoRepository {

    private static final String COLUNAS = """
            id, nome, tipo, rentabilidade_bp, risco, liquidez, prazo_min_meses, prazo_max_meses,
            valor_min_centavos, valor_max_centavos, isento_ir, carencia_meses, ativo, descricao
            """;

    private final DataSource dataSource;

    @Inject
    public ProdutoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Produto> listarAtivos() {
        return consultar("SELECT " + COLUNAS + " FROM produto WHERE ativo = 1 ORDER BY id", null);
    }

    public List<Produto> listarAtivosPorTipo(String tipo) {
        return consultar("SELECT " + COLUNAS + " FROM produto "
                + "WHERE ativo = 1 AND tipo = ? ORDER BY id", tipo);
    }

    public Optional<Produto> buscarAtivo(long id) {
        String sql = "SELECT " + COLUNAS + " FROM produto WHERE ativo = 1 AND id = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar produto " + id, e);
        }
    }

    /** Tipos distintos disponiveis, usados na mensagem de erro de tipo invalido. */
    public List<String> tiposDisponiveis() {
        String sql = "SELECT DISTINCT tipo FROM produto WHERE ativo = 1 ORDER BY tipo";
        List<String> tipos = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.add(rs.getString("tipo"));
            }
            return tipos;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar tipos de produto", e);
        }
    }

    private List<Produto> consultar(String sql, String tipo) {
        List<Produto> produtos = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            if (tipo != null) {
                ps.setString(1, tipo);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs));
                }
            }
            return produtos;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao consultar produtos", e);
        }
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        int prazoMaximo = rs.getInt("prazo_max_meses");
        Integer prazoMaximoMeses = rs.wasNull() ? null : prazoMaximo;

        long valorMaximoCentavos = rs.getLong("valor_max_centavos");
        BigDecimal valorMaximo = rs.wasNull() ? null : Dinheiro.deCentavos(valorMaximoCentavos);

        return new Produto(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("tipo"),
                Dinheiro.taxaDeBasisPoints(rs.getInt("rentabilidade_bp")),
                Risco.de(rs.getString("risco")),
                Liquidez.de(rs.getString("liquidez")),
                rs.getInt("prazo_min_meses"),
                prazoMaximoMeses,
                Dinheiro.deCentavos(rs.getLong("valor_min_centavos")),
                valorMaximo,
                rs.getInt("isento_ir") != 0,
                rs.getInt("carencia_meses"),
                rs.getInt("ativo") != 0,
                rs.getString("descricao"));
    }
}
