package br.com.investimentos.persistencia;

import br.com.investimentos.dominio.Cliente;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ClienteRepository {

    private final DataSource dataSource;

    @Inject
    public ClienteRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Optional<Cliente> buscar(long id) {
        String sql = "SELECT id, nome FROM cliente WHERE id = ?";
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new Cliente(rs.getLong("id"), rs.getString("nome")))
                        : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao buscar cliente " + id, e);
        }
    }

    public List<Cliente> listar() {
        String sql = "SELECT id, nome FROM cliente ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexao = dataSource.getConnection();
             PreparedStatement ps = conexao.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clientes.add(new Cliente(rs.getLong("id"), rs.getString("nome")));
            }
            return clientes;
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao listar clientes", e);
        }
    }
}
