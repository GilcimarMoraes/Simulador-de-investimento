package br.com.investimentos.infra;

import br.com.investimentos.seguranca.PasswordHasher;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Inicializacao do banco: cria o diretorio do arquivo SQLite, aplica o schema e os dados de
 * demonstracao e cadastra os usuarios da API (senhas com PBKDF2, nunca em texto puro).
 *
 * <p>O schema e idempotente (CREATE TABLE IF NOT EXISTS) e o seed usa ids fixos com
 * INSERT OR IGNORE, portanto reiniciar a aplicacao preserva os dados ja gravados.</p>
 */
@ApplicationScoped
public class DatabaseBootstrap {

    private static final Logger LOG = Logger.getLogger(DatabaseBootstrap.class);

    private static final List<String> TABELAS = List.of(
            "requisicao_log", "simulacao", "movimentacao", "investimento", "produto", "usuario", "cliente");

    @Inject
    DataSource dataSource;

    @Inject
    PasswordHasher passwordHasher;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String urlJdbc;

    @ConfigProperty(name = "app.db.recreate", defaultValue = "false")
    boolean recriar;

    @ConfigProperty(name = "app.db.seed", defaultValue = "true")
    boolean semear;

    void aoIniciar(@Observes StartupEvent evento) {
        prepararDiretorio();
        try (Connection conexao = dataSource.getConnection()) {
            conexao.setAutoCommit(false);
            if (recriar) {
                LOG.warn("app.db.recreate=true: recriando todas as tabelas");
                for (String tabela : TABELAS) {
                    executar(conexao, "DROP TABLE IF EXISTS " + tabela);
                }
            }
            executarScript(conexao, lerRecurso("db/schema.sql"));
            if (semear) {
                executarScript(conexao, lerRecurso("db/seed.sql"));
                semearUsuarios(conexao);
            }
            conexao.commit();
            LOG.info("Banco inicializado com sucesso.");
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao inicializar o banco de dados SQLite", e);
        }
    }

    /**
     * O driver SQLite nao cria o diretorio do arquivo. Como o pool abre a conexao somente
     * quando solicitado, garantimos o diretorio antes da primeira conexao.
     */
    private void prepararDiretorio() {
        if (urlJdbc == null || !urlJdbc.startsWith("jdbc:sqlite:")) {
            return;
        }
        String caminho = urlJdbc.substring("jdbc:sqlite:".length());
        int interrogacao = caminho.indexOf('?');
        if (interrogacao >= 0) {
            caminho = caminho.substring(0, interrogacao);
        }
        if (caminho.isBlank() || caminho.startsWith(":memory:") || caminho.startsWith("file:")) {
            return;
        }
        Path arquivo = Path.of(caminho).toAbsolutePath();
        Path diretorio = arquivo.getParent();
        if (diretorio != null) {
            try {
                Files.createDirectories(diretorio);
            } catch (IOException e) {
                throw new IllegalStateException("Nao foi possivel criar o diretorio do banco: " + diretorio, e);
            }
        }
    }

    private void semearUsuarios(Connection conexao) throws SQLException {
        criarUsuario(conexao, "demo", "demo123", "USER", 123L);
        criarUsuario(conexao, "admin", "admin123", "ADMIN,USER", 123L);
        criarUsuario(conexao, "ana", "ana123", "USER", 1L);
    }

    private void criarUsuario(Connection conexao, String username, String senha, String roles, Long clienteId)
            throws SQLException {
        String sql = "INSERT OR IGNORE INTO usuario (username, senha_hash, roles, cliente_id, ativo) "
                + "VALUES (?, ?, ?, ?, 1)";
        try (PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHasher.gerar(senha));
            ps.setString(3, roles);
            ps.setObject(4, clienteId);
            ps.executeUpdate();
        }
    }

    private void executarScript(Connection conexao, List<String> comandos) throws SQLException {
        for (String comando : comandos) {
            executar(conexao, comando);
        }
    }

    private void executar(Connection conexao, String comando) throws SQLException {
        try (Statement statement = conexao.createStatement()) {
            statement.execute(comando);
        }
    }

    private List<String> lerRecurso(String caminho) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (InputStream entrada = loader.getResourceAsStream(caminho)) {
            if (entrada == null) {
                throw new IllegalStateException("Recurso SQL nao encontrado: " + caminho);
            }
            return ScriptSql.comandos(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler recurso SQL: " + caminho, e);
        }
    }
}
