package br.com.investimentos.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    @DisplayName("Hash nao expoe a senha e valida a senha correta")
    void hashEValidacao() {
        String hash = hasher.gerar("demo123");

        assertFalse(hash.contains("demo123"));
        assertTrue(hash.startsWith("pbkdf2-sha256$210000$"));
        assertTrue(hasher.verificar("demo123", hash));
        assertFalse(hasher.verificar("demo124", hash));
    }

    @Test
    @DisplayName("Salt aleatorio produz hashes diferentes para a mesma senha")
    void saltAleatorio() {
        assertNotEquals(hasher.gerar("demo123"), hasher.gerar("demo123"));
    }

    @Test
    @DisplayName("Hashes malformados ou nulos sao recusados sem excecao")
    void hashesInvalidos() {
        assertFalse(hasher.verificar("demo123", null));
        assertFalse(hasher.verificar(null, "qualquer"));
        assertFalse(hasher.verificar("demo123", "texto-puro"));
        assertFalse(hasher.verificar("demo123", "pbkdf2-sha256$abc$aaa$bbb"));
    }
}
