package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.Liquidez;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Posicoes de investimento do cliente (entrada do motor de perfil e do endpoint de historico). */
@ApplicationScoped
public class InvestimentoRepository {

    private final DataSource dataSource;

    @Inject
    public InvestimentoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Investimento> listarPorCliente(long clienteId) {
        return consultar("SELECT id, cliente_id, tipo, valor_centavos, rentabilidade_bp, liquidez, data "
                + "FROM investimento WHERE cliente_id = ? ORDER BY data DESC, id DESC", clienteId);
    }

    private List<Investimento> consultar(String sql, long clienteId) {
        List<Investimento> investimentos = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal valor = Dinheiro.deCentavos(rs.getLong("valor_centavos"));
                    BigDecimal rentabilidade = Dinheiro.taxaDeBasisPoints(rs.getInt("rentabilidade_bp"));
                    investimentos.add(new Investimento(
                            rs.getLong("id"),
                            rs.getLong("cliente_id"),
                            rs.getString("tipo"),
                            valor,
                            rentabilidade,
                            Liquidez.de(rs.getString("liquidez")),
                            rs.getObject("data", LocalDate.class)));
                }
            }
            return investimentos;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar investimentos do cliente " + clienteId, e);
        }
    }
}
