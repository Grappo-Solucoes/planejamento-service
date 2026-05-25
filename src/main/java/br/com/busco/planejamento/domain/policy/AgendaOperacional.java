package br.com.busco.planejamento.domain.policy;

import br.com.busco.planejamento.sk.ids.MotoristaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

public interface AgendaOperacional {
    boolean motoristaDisponivel(MotoristaId motorista, LocalDateTime data);
    boolean veiculoDisponivel(VeiculoId veiculoId, LocalDateTime data);

    @Component
    public static class Impl implements AgendaOperacional {

        @Override
        public boolean motoristaDisponivel(MotoristaId motorista, LocalDateTime data) {
            return false;
        }

        @Override
        public boolean veiculoDisponivel(VeiculoId veiculoId, LocalDateTime data) {
            return false;
        }
    }
}
