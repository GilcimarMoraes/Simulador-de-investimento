package br.com.investimentos.persistencia;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/** Contagem de movimentacoes usada no criterio de frequencia do motor de perfil. */
@ApplicationScoped
public class MovimentacaoRepository {

    private final DataSource dataSource;

    @Inject
    public MovimentacaoRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int contarDesde(long clienteId, LocalDate dataInicial) {
        String sql = "SELECT COUNT(*) AS total FROM movimentacao WHERE cliente_id = ? AND data >= ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, clienteId);
            ps.setObject(2, dataInicial);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao contar movimentacoes do cliente " + clienteId, e);
        }
    }
}
