package br.com.busco.planejamento.domain.policy;


import br.com.busco.planejamento.domain.AgendamentoOperacional;
import br.com.busco.planejamento.domain.AgendamentoRepository;
import br.com.busco.planejamento.domain.Periodo;
import br.com.busco.planejamento.sk.ids.AgendamentoOperacionalId;
import br.com.busco.planejamento.sk.ids.RotaId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class  PoliticaDeConflitoTemporal {

    private final AgendamentoRepository repository;

    public void validar(AgendamentoOperacional agendamento) {

//        boolean existeConflito =
//                repository.existeSobreposicao(
//                        agendamento.getRota(),
//                        intervalo.getInicio(),
//                        intervalo.getFim(),
//                        agendamento.getId()
//                );
//
//        if (existeConflito) {
//            throw new ConflitoDeAgendaException(
//                    "Já existe agendamento nesse horário para a rota"
//            );
//        }
    }
}