package br.com.investimentos.servico;

import br.com.investimentos.api.dto.TokenResponse;
import br.com.investimentos.dominio.Usuario;
import br.com.investimentos.dominio.excecao.CredenciaisInvalidasException;
import br.com.investimentos.persistencia.UsuarioRepository;
import br.com.investimentos.seguranca.PasswordHasher;
import br.com.investimentos.seguranca.TokenService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.Optional;

/**
 * Autenticacao por usuario/senha com emissao de JWT.
 *
 * <p>A verificação é feita sempre (inclusive para usuário inexistente, com um hash fictício)
 * para reduzir a diferença de tempo entre "usuário não existe" e "senha errada".</p>
 */
@ApplicationScoped
public class AutenticacaoService {

    private final UsuarioRepository usuarios;
    private final PasswordHasher hasher;
    private final TokenService tokens;
    private final String hashFicticio;

    @Inject
    public AutenticacaoService(UsuarioRepository usuarios, PasswordHasher hasher, TokenService tokens) {
        this.usuarios = usuarios;
        this.hasher = hasher;
        this.tokens = tokens;
        this.hashFicticio = hasher.gerar("senha-ficticia-para-tempo-constante");
    }

    public TokenResponse autenticar(String username, String password) {
        Optional<Usuario> encontrado = usuarios.buscarPorUsername(username);
        boolean senhaConfere = encontrado
                .map(usuario -> hasher.verificar(password, usuario.senhaHash()))
                .orElseGet(() -> {
                    hasher.verificar(password, hashFicticio);
                    return false;
                });

        Usuario usuario = encontrado.filter(Usuario::ativo).orElse(null);
        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException();
        }

        Instant emissao = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        String token = tokens.gerar(usuario, emissao);
        return new TokenResponse(
                token,
                "Bearer",
                tokens.ttlSegundos(),
                usuario.username(),
                usuario.roles(),
                usuario.clienteId(),
                tokens.expiracao(emissao));
    }
}
