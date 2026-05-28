package br.com.busco.planejamento.domain.services;

import br.com.busco.planejamento.domain.*;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpansaoPlanejamentoDomainService {

    private final AgendamentoRepository agendamentoRepository;

    @Transactional
    public ExpansaoResultado expandir(PlanejamentoLote planejamento, LocalDateTime dataReferencia) {
        if (planejamento.getStatus() != StatusPlanejamentoLote.ATIVO) {
            throw new IllegalStateException("Apenas planejamentos ativos podem ser expandidos");
        }

        List<LocalDateTime> datas = planejamento.gerarDatas();
        List<AgendamentoOperacional> agendamentosCriados = new ArrayList<>();
        List<FalhaExpansao> falhas = new ArrayList<>();

        for (LocalDateTime data : datas) {
            if (data.isBefore(dataReferencia)) {
                continue; // Não gerar agendamentos no passado
            }

            try {
                AgendamentoOperacional agendamento = AgendamentoOperacional.builder()
                        .rota(planejamento.getRota())
                        .veiculo(planejamento.getVeiculo())
                        .motorista(planejamento.getMotorista())
                        .data(data)
                        .origem(OrigemAgendamento.lote(planejamento.getId()))
                        .capacidadeVeiculo(buscarCapacidadeVeiculo(planejamento.getVeiculo()))
                        .build();

                agendamentoRepository.save(agendamento);
                agendamentosCriados.add(agendamento);

            } catch (Exception e) {
                log.error("Erro ao criar agendamento para data {}: {}", data, e.getMessage());
                falhas.add(new FalhaExpansao(data, e.getMessage()));
            }
        }

        ExpansaoResultado resultado = new ExpansaoResultado(agendamentosCriados, falhas);

        //TODO: publicar os eventos da saga
//        if (resultado.getSucesso()) {
//            eventPublisher.publish(PlanejamentoExpandidoEvent.of(planejamento, agendamentosCriados));
//        } else if (!agendamentosCriados.isEmpty()) {
//            eventPublisher.publish(PlanejamentoExpandidoParcialmenteEvent.of(planejamento, resultado));
//        } else {
//            eventPublisher.publish(PlanejamentoExpandidoFalhouEvent.of(planejamento, falhas));
//        }

        return resultado;
    }

    private CapacidadeVeiculo buscarCapacidadeVeiculo(VeiculoId veiculoId) {
        return CapacidadeVeiculo.of(44, 0, 0, 0);
//        // Integração com serviço de frota
//        // Cache por 5 minutos para evitar muitas chamadas
//        return capacidadeVeiculoCache.get(veiculoId);
    }

    public record ExpansaoResultado(
            List<AgendamentoOperacional> agendamentosCriados,
            List<FalhaExpansao> falhas
    ) {
        public boolean getSucesso() {
            return falhas.isEmpty() && !agendamentosCriados.isEmpty();
        }

        public boolean getParcial() {
            return !falhas.isEmpty() && !agendamentosCriados.isEmpty();
        }
    }

    public record FalhaExpansao(LocalDateTime data, String motivo) {}

}
