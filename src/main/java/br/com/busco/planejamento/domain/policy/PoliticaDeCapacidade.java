package br.com.busco.planejamento.domain.policy;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PoliticaDeCapacidade {

    private final CapacidadeOperacional capacidade;

    public void validar(AgendamentoOperacional agendamento) {

        int capacidadeVeiculo =
                capacidade.capacidadeVeiculo(agendamento.getVeiculo());

        int alunosRota =
                capacidade.quantidadeAlunosNaRota(agendamento.getRota());

//        if (capacidadeVeiculo < alunosRota) {
//            throw new CapacidadeInsuficienteException();
//        }
    }
}