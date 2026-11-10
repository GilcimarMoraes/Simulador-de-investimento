package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.Usuario;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UsuarioRepository {

    private final DataSource dataSource;

    @Inject
    public UsuarioRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Usuario> buscarPorUsername(String username) {
        String sql = "SELECT id, username, senha_hash, roles, cliente_id, ativo "
                + "FROM usuario WHERE username = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                String roles = rs.getString("roles");
                List<String> papeis = roles == null || roles.isBlank()
                        ? List.of()
                        : Arrays.stream(roles.split(",")).map(String::trim).filter(r -> !r.isEmpty()).toList();
                Long clienteId = rs.getObject("cliente_id", Long.class);
                return Optional.of(new Usuario(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("senha_hash"),
                        papeis,
                        clienteId,
                        rs.getBoolean("ativo")));
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar usuario " + username, e);
        }
    }
}
