package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.RegistroRequisicao;
import br.com.investimentos.dominio.ServicoTelemetria;
import br.com.investimentos.infra.Datas;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Persistencia e agregacao das amostras de telemetria das chamadas HTTP. */
@ApplicationScoped
public class TelemetriaRepository {

    private final DataSource dataSource;

    @Inject
    public TelemetriaRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void registrar(RegistroRequisicao registro) {
        String sql = """
                INSERT INTO requisicao_log (servico, metodo, caminho, status, duracao_ms, usuario, data_hora)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, registro.servico());
            ps.setString(2, registro.metodo());
            ps.setString(3, registro.caminho());
            ps.setInt(4, registro.status());
            ps.setLong(5, registro.duracaoMs());
            ps.setString(6, registro.usuario());
            ps.setString(7, Datas.formatar(registro.dataHora()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao registrar telemetria", e);
        }
    }

    /**
     * Agrega volume, tempo medio, p95, tempo maximo e erros por servico no periodo.
     * O p95 e calculado por posicao na lista ordenada de duracoes (sem funcao de percentil nativa).
     */
    public List<ServicoTelemetria> agregar(LocalDate inicio, LocalDate fimExclusivo) {
        String inicioIso = Datas.inicioDoDia(inicio);
        String fimIso = Datas.inicioDoDia(fimExclusivo);
        String sql = """
                SELECT servico,
                       COUNT(*) AS quantidade,
                       CAST(ROUND(AVG(duracao_ms)) AS INTEGER) AS media,
                       MIN(duracao_ms) AS minimo,
                       MAX(duracao_ms) AS maximo,
                       SUM(CASE WHEN status >= 400 THEN 1 ELSE 0 END) AS erros
                FROM requisicao_log
                WHERE data_hora >= ? AND data_hora < ?
                GROUP BY servico
                ORDER BY servico
                """;
        List<ServicoTelemetria> servicos = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, inicioIso);
            ps.setString(2, fimIso);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String servico = rs.getString("servico");
                    long quantidade = rs.getLong("quantidade");
                    servicos.add(new ServicoTelemetria(
                            servico,
                            quantidade,
                            rs.getLong("media"),
                            p95(conexao, servico, inicioIso, fimIso, quantidade),
                            rs.getLong("maximo"),
                            rs.getLong("erros")));
                }
            }
            return servicos;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao agregar telemetria", e);
        }
    }

    private long p95(Connection conexao, String servico, String inicio, String fim, long quantidade)
            throws SQLException {
        if (quantidade <= 0) {
            return 0;
        }
        int posicao = (int) Math.min(quantidade - 1, Math.max(0, Math.ceil(quantidade * 0.95) - 1));
        String sql = """
                SELECT duracao_ms FROM requisicao_log
                WHERE servico = ? AND data_hora >= ? AND data_hora < ?
                ORDER BY duracao_ms
                LIMIT 1 OFFSET ?
                """;
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, servico);
            ps.setString(2, inicio);
            ps.setString(3, fim);
            ps.setInt(4, posicao);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("duracao_ms") : 0;
            }
        }
    }
}
