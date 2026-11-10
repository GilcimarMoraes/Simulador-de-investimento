package br.com.investimentos.servico;

import br.com.investimentos.dominio.Cliente;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.excecao.RecursoNaoEncontradoException;
import br.com.investimentos.persistencia.ClienteRepository;
import br.com.investimentos.persistencia.InvestimentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/** Consultas de clientes e do historico de investimentos. */
@ApplicationScoped
public class ClienteService {

    private final ClienteRepository clientes;
    private final InvestimentoRepository investimentos;

    @Inject
    public ClienteService(ClienteRepository clientes, InvestimentoRepository investimentos) {
        this.clientes = clientes;
        this.investimentos = investimentos;
    }

    public List<Cliente> listar() {
        return clientes.listar();
    }

    public List<Investimento> investimentos(long clienteId) {
        clientes.buscar(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente %d não encontrado.".formatted(clienteId)));
        return investimentos.listarPorCliente(clienteId);
    }
}
