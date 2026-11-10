package br.com.investimentos.seguranca;

import br.com.investimentos.dominio.Usuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Emissao de JWT RS256. A chave privada e configurada em {@code smallrye.jwt.sign.key.location}
 * (sobrescrevivel por variavel de ambiente em producao).
 */
@ApplicationScoped
public class TokenService {

    private final String issuer;
    private final long ttlSegundos;

    public TokenService(
            @ConfigProperty(name = "app.jwt.issuer", defaultValue = "painel-investimentos") String issuer,
            @ConfigProperty(name = "app.jwt.ttl-seconds", defaultValue = "3600") long ttlSegundos) {
        this.issuer = issuer;
        this.ttlSegundos = ttlSegundos;
    }

    public long ttlSegundos() {
        return ttlSegundos;
    }

    public Instant expiracao(Instant emissao) {
        return emissao.plusSeconds(ttlSegundos);
    }

    public String gerar(Usuario usuario, Instant emissao) {
        var claims = Jwt.claims()
                .issuer(issuer)
                .subject(usuario.username())
                .upn(usuario.username())
                .claim("preferred_username", usuario.username())
                .claim("groups", usuario.roles())
                .issuedAt(emissao)
                .expiresAt(expiracao(emissao))
                .claim("jti", UUID.randomUUID().toString());
        if (usuario.clienteId() != null) {
            claims.claim("cliente_id", usuario.clienteId());
        }
        return claims.sign();
    }
}
