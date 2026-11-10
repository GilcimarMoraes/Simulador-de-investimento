package br.com.investimentos.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Apoio exclusivo de testes: limpa as tabelas volateis entre cenarios. */
@ApplicationScoped
public class LimpezaBanco {

    @Inject
    DataSource dataSource;

    @Transactional
    public void executar() {
        try (Connection conexao = dataSource.getConnection();
             Statement statement = conexao.createStatement()) {
            statement.executeUpdate("DELETE FROM requisicao_log");
            statement.executeUpdate("DELETE FROM simulacao");
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao limpar banco de teste", e);
        }
    }
}
