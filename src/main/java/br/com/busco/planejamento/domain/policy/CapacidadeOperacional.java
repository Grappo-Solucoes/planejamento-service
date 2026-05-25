package br.com.busco.planejamento.domain.policy;

import br.com.busco.planejamento.sk.ids.RotaId;
import br.com.busco.planejamento.sk.ids.VeiculoId;
import org.springframework.stereotype.Component;

public interface CapacidadeOperacional {
    int capacidadeVeiculo(VeiculoId veiculoId);
    int quantidadeAlunosNaRota(RotaId rotaId);

    @Component
    public static class Impl implements CapacidadeOperacional {

        @Override
        public int capacidadeVeiculo(VeiculoId veiculoId) {
            return 0;
        }

        @Override
        public int quantidadeAlunosNaRota(RotaId rotaId) {
            return 0;
        }
    }

}
