package br.com.investimentos.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerfilRiscoTest {

    @Test
    @DisplayName("Interpreta o perfil com variacoes de caixa e acento")
    void interpretacaoTolerante() {
        assertEquals(PerfilRisco.MODERADO, PerfilRisco.de("moderado"));
        assertEquals(PerfilRisco.MODERADO, PerfilRisco.de("Moderado"));
        assertEquals(PerfilRisco.AGRESSIVO, PerfilRisco.de("AGRESSIVO"));
        assertEquals(PerfilRisco.CONSERVADOR, PerfilRisco.de(" conservador "));
    }

    @Test
    @DisplayName("Perfil desconhecido e rejeitado com mensagem util")
    void perfilInvalido() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> PerfilRisco.de("Arrojado"));
        assertTrue(erro.getMessage().contains("Conservador, Moderado, Agressivo"));
    }

    @Test
    @DisplayName("Compatibilidade de risco por perfil")
    void compatibilidade() {
        assertTrue(PerfilRisco.CONSERVADOR.aceita(Risco.BAIXO));
        assertFalse(PerfilRisco.CONSERVADOR.aceita(Risco.MEDIO));
        assertFalse(PerfilRisco.CONSERVADOR.aceita(Risco.ALTO));

        assertTrue(PerfilRisco.MODERADO.aceita(Risco.BAIXO));
        assertTrue(PerfilRisco.MODERADO.aceita(Risco.MEDIO));
        assertFalse(PerfilRisco.MODERADO.aceita(Risco.ALTO));

        assertTrue(PerfilRisco.AGRESSIVO.aceita(Risco.ALTO));
    }

    @Test
    @DisplayName("Aderencia prioriza o risco esperado de cada perfil")
    void aderencia() {
        assertEquals(3, PerfilRisco.CONSERVADOR.aderencia(Risco.BAIXO));
        assertEquals(3, PerfilRisco.MODERADO.aderencia(Risco.MEDIO));
        assertEquals(3, PerfilRisco.AGRESSIVO.aderencia(Risco.ALTO));
        assertEquals(0, PerfilRisco.CONSERVADOR.aderencia(Risco.ALTO));
    }
}
