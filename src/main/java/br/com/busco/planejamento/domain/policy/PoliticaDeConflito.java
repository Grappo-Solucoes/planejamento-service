package br.com.busco.planejamento.domain.policy;

import br.com.busco.planejamento.domain.AgendamentoOperacional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PoliticaDeConflito {
    private final AgendaOperacional agenda;

    public void validar(AgendamentoOperacional agendamento) {

//        if (!agenda.motoristaDisponivel(
//                agendamento.getMotorista(),
//                agendamento.getData()
//        )) {
//            throw new ConflitoDeMotoristaException();
//        }
//
//        if (!agenda.veiculoDisponivel(
//                agendamento.getVeiculo(),
//                agendamento.getData()
//        )) {
//            throw new ConflitoDeVeiculoException();
//        }
    }

}
