package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.AgregadoProdutoDia;
import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.PerfilRisco;
import br.com.investimentos.dominio.Simulacao;
import br.com.investimentos.infra.Datas;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Persistencia e consultas agregadas das simulacoes realizadas em PostgreSQL. */
@ApplicationScoped
public class SimulacaoRepository {

    private static final String COLUNAS = """
            id, cliente_id, produto_id, produto_nome, tipo_produto, valor_investido_centavos,
            valor_final_centavos, valor_final_liquido_centavos, imposto_centavos,
            rentabilidade_efetiva_bp, prazo_meses, perfil_cliente, data_simulacao
            """;

    private final DataSource dataSource;

    @Inject
    public SimulacaoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public long inserir(Simulacao simulacao) {
        String sql = """
                INSERT INTO simulacao (cliente_id, produto_id, produto_nome, tipo_produto,
                    valor_investido_centavos, valor_final_centavos, valor_final_liquido_centavos,
                    imposto_centavos, rentabilidade_efetiva_bp, prazo_meses, perfil_cliente, data_simulacao)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, simulacao.clienteId());
            ps.setLong(2, simulacao.produtoId());
            ps.setString(3, simulacao.produtoNome());
            ps.setString(4, simulacao.tipoProduto());
            ps.setLong(5, Dinheiro.paraCentavos(simulacao.valorInvestido()));
            ps.setLong(6, Dinheiro.paraCentavos(simulacao.valorFinal()));
            ps.setLong(7, Dinheiro.paraCentavos(simulacao.valorFinalLiquido()));
            ps.setLong(8, Dinheiro.paraCentavos(simulacao.impostoRendimento()));
            ps.setInt(9, Dinheiro.basisPointsDe(simulacao.rentabilidadeEfetiva()));
            ps.setInt(10, simulacao.prazoMeses());
            ps.setString(11, simulacao.perfilCliente().name());
            ps.setObject(12, Datas.paraTimestamp(simulacao.dataSimulacao()));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
            throw new PersistenciaException("Banco nao retornou o id da simulacao", null);
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao persistir simulacao", e);
        }
    }

    /** Lista simulacoes; {@code clienteId} nulo retorna todos os clientes. */
    public List<Simulacao> listar(Long clienteId) {
        boolean filtrar = clienteId != null;
        String sql = "SELECT " + COLUNAS + " FROM simulacao "
                + (filtrar ? "WHERE cliente_id = ? " : "")
                + "ORDER BY data_simulacao DESC, id DESC";
        List<Simulacao> simulacoes = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            if (filtrar) {
                ps.setLong(1, clienteId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    simulacoes.add(mapear(rs));
                }
            }
            return simulacoes;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar simulacoes", e);
        }
    }

    /**
     * Agrega quantidade e media de valor final por produto e dia.
     * O dia e derivado de {@code data_simulacao} no fuso UTC, independente do timezone da sessao.
     */
    public List<AgregadoProdutoDia> porProdutoDia(LocalDate inicio, LocalDate fimExclusivo) {
        String sql = """
                SELECT produto_nome,
                       (data_simulacao AT TIME ZONE 'UTC')::date AS dia,
                       COUNT(*) AS quantidade,
                       ROUND(AVG(valor_final_centavos))::bigint AS media_centavos
                FROM simulacao
                WHERE data_simulacao >= ? AND data_simulacao < ?
                GROUP BY produto_nome, (data_simulacao AT TIME ZONE 'UTC')::date
                ORDER BY dia DESC, produto_nome
                """;
        List<AgregadoProdutoDia> agregados = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setObject(1, Datas.inicioDoDia(inicio));
            ps.setObject(2, Datas.inicioDoDia(fimExclusivo));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal media = Dinheiro.deCentavos(rs.getLong("media_centavos"));
                    agregados.add(new AgregadoProdutoDia(
                            rs.getString("produto_nome"),
                            rs.getObject("dia", LocalDate.class),
                            rs.getLong("quantidade"),
                            media));
                }
            }
            return agregados;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao agregar simulacoes por produto", e);
        }
    }

    private Simulacao mapear(ResultSet rs) throws SQLException {
        OffsetDateTime dataSimulacao = rs.getObject("data_simulacao", OffsetDateTime.class);
        return new Simulacao(
                rs.getLong("id"),
                rs.getLong("cliente_id"),
                rs.getLong("produto_id"),
                rs.getString("produto_nome"),
                rs.getString("tipo_produto"),
                Dinheiro.deCentavos(rs.getLong("valor_investido_centavos")),
                Dinheiro.deCentavos(rs.getLong("valor_final_centavos")),
                Dinheiro.deCentavos(rs.getLong("valor_final_liquido_centavos")),
                Dinheiro.deCentavos(rs.getLong("imposto_centavos")),
                Dinheiro.taxaDeBasisPoints(rs.getInt("rentabilidade_efetiva_bp")),
                rs.getInt("prazo_meses"),
                PerfilRisco.valueOf(rs.getString("perfil_cliente")),
                Datas.paraInstante(dataSimulacao));
    }
}
