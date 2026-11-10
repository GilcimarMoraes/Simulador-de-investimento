package br.com.investimentos.servico;

import br.com.investimentos.dominio.RegistroRequisicao;
import br.com.investimentos.dominio.ServicoTelemetria;
import br.com.investimentos.dominio.TelemetriaResumo;
import br.com.investimentos.persistencia.TelemetriaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.List;

/**
 * Telemetria de chamadas HTTP. O registro nunca pode derrubar a requisicao original:
 * falhas sao apenas logadas e a resposta ao cliente segue normalmente.
 */
@ApplicationScoped
public class TelemetriaService {

    private static final Logger LOG = Logger.getLogger(TelemetriaService.class);

    private final TelemetriaRepository repositorio;

    @Inject
    public TelemetriaService(TelemetriaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void registrar(RegistroRequisicao registro) {
        try {
            repositorio.registrar(registro);
        } catch (RuntimeException e) {
            LOG.warnf(e, "Falha ao registrar telemetria do servico %s", registro.servico());
        }
    }

    public TelemetriaResumo consultar(LocalDate inicio, LocalDate fim) {
        LocalDate fimExclusivo = fim.plusDays(1);
        List<ServicoTelemetria> servicos = repositorio.agregar(inicio, fimExclusivo);
        return new TelemetriaResumo(servicos, inicio, fim);
    }
}
