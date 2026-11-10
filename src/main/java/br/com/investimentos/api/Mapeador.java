package br.com.investimentos.api;

import br.com.investimentos.api.dto.FatorPerfilDTO;
import br.com.investimentos.api.dto.InvestimentoDTO;
import br.com.investimentos.api.dto.PerfilRiscoResponse;
import br.com.investimentos.api.dto.ProdutoDTO;
import br.com.investimentos.api.dto.ProdutoRecomendadoDTO;
import br.com.investimentos.api.dto.ProdutoValidadoDTO;
import br.com.investimentos.api.dto.ResultadoSimulacaoDTO;
import br.com.investimentos.api.dto.PeriodoDTO;
import br.com.investimentos.api.dto.ServicoTelemetriaDTO;
import br.com.investimentos.api.dto.SimulacaoHistoricoDTO;
import br.com.investimentos.api.dto.SimulacaoPorProdutoDiaDTO;
import br.com.investimentos.api.dto.SimulacaoResponse;
import br.com.investimentos.api.dto.TelemetriaResponse;
import br.com.investimentos.dominio.AgregadoProdutoDia;
import br.com.investimentos.dominio.AnalisePerfil;
import br.com.investimentos.dominio.Dinheiro;
import br.com.investimentos.dominio.Investimento;
import br.com.investimentos.dominio.Produto;
import br.com.investimentos.dominio.ResultadoCalculo;
import br.com.investimentos.dominio.ServicoTelemetria;
import br.com.investimentos.dominio.Simulacao;
import br.com.investimentos.dominio.TelemetriaResumo;
import br.com.investimentos.servico.SimulacaoRealizada;

import java.math.BigDecimal;

/** Conversao entre dominio e envelopes JSON da API. */
public final class Mapeador {

    private Mapeador() {
    }

    public static ProdutoValidadoDTO produtoValidado(Produto produto) {
        return new ProdutoValidadoDTO(
                produto.id(),
                produto.nome(),
                produto.tipo(),
                Dinheiro.taxaCompacta(produto.rentabilidadeAnual()),
                produto.risco().descricao());
    }

    public static ResultadoSimulacaoDTO resultado(ResultadoCalculo calculo, BigDecimal valor, int prazoMeses) {
        return new ResultadoSimulacaoDTO(
                calculo.valorFinal(),
                calculo.rentabilidadeEfetiva(),
                prazoMeses,
                valor,
                calculo.valorFinalLiquido(),
                calculo.imposto(),
                calculo.rentabilidadeLiquidaEfetiva());
    }

    public static SimulacaoResponse simulacao(SimulacaoRealizada realizada) {
        Simulacao simulacao = realizada.simulacao();
        return new SimulacaoResponse(
                produtoValidado(realizada.produto()),
                resultado(realizada.calculo(), simulacao.valorInvestido(), simulacao.prazoMeses()),
                simulacao.dataSimulacao(),
                simulacao.id(),
                simulacao.clienteId(),
                simulacao.perfilCliente().descricao(),
                realizada.alertas());
    }

    public static SimulacaoHistoricoDTO historico(Simulacao simulacao) {
        return new SimulacaoHistoricoDTO(
                simulacao.id(),
                simulacao.clienteId(),
                simulacao.produtoNome(),
                simulacao.valorInvestido(),
                simulacao.valorFinal(),
                simulacao.prazoMeses(),
                simulacao.dataSimulacao());
    }

    public static SimulacaoPorProdutoDiaDTO agregado(AgregadoProdutoDia agregado) {
        return new SimulacaoPorProdutoDiaDTO(
                agregado.produto(),
                agregado.data(),
                agregado.quantidadeSimulacoes(),
                agregado.mediaValorFinal());
    }

    public static PerfilRiscoResponse perfil(AnalisePerfil analise) {
        return new PerfilRiscoResponse(
                analise.clienteId(),
                analise.perfil().descricao(),
                analise.pontuacao(),
                analise.descricao(),
                analise.fatores().stream()
                        .map(f -> new FatorPerfilDTO(f.criterio(), f.pontos(), f.maximo(), f.detalhe()))
                        .toList(),
                analise.totalInvestido(),
                analise.percentualLiquidez(),
                analise.movimentacoes180Dias(),
                analise.rentabilidadeMedia());
    }

    public static ProdutoRecomendadoDTO recomendado(Produto produto) {
        return new ProdutoRecomendadoDTO(
                produto.id(),
                produto.nome(),
                produto.tipo(),
                Dinheiro.taxaCompacta(produto.rentabilidadeAnual()),
                produto.risco().descricao(),
                produto.liquidez().descricao(),
                produto.prazoMinimoMeses(),
                produto.prazoMaximoMeses(),
                produto.valorMinimo(),
                produto.valorMaximo());
    }

    public static ProdutoDTO produto(Produto produto) {
        return new ProdutoDTO(
                produto.id(),
                produto.nome(),
                produto.tipo(),
                Dinheiro.taxaCompacta(produto.rentabilidadeAnual()),
                produto.risco().descricao(),
                produto.liquidez().descricao(),
                produto.prazoMinimoMeses(),
                produto.prazoMaximoMeses(),
                produto.valorMinimo(),
                produto.valorMaximo(),
                produto.isentoIr(),
                produto.carenciaMeses(),
                produto.ativo(),
                produto.descricao());
    }

    public static InvestimentoDTO investimento(Investimento investimento) {
        return new InvestimentoDTO(
                investimento.id(),
                investimento.tipo(),
                investimento.valor(),
                Dinheiro.taxaCompacta(investimento.rentabilidadeAnual()),
                investimento.data());
    }

    public static ServicoTelemetriaDTO servico(ServicoTelemetria servico) {
        return new ServicoTelemetriaDTO(
                servico.nome(),
                servico.quantidadeChamadas(),
                servico.mediaTempoRespostaMs(),
                servico.p95TempoRespostaMs(),
                servico.maxTempoRespostaMs(),
                servico.erros());
    }

    public static TelemetriaResponse telemetria(TelemetriaResumo resumo) {
        return new TelemetriaResponse(
                resumo.servicos().stream().map(Mapeador::servico).toList(),
                new PeriodoDTO(resumo.inicio(), resumo.fim()));
    }
}
