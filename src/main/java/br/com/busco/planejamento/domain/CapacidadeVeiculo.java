package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CapacidadeVeiculo implements ValueObject {

    private int passageirosSentados;
    private int passageirosEmPe;
    private int cadeirantes;
    private int totalEspecialNecessidades;

    public static CapacidadeVeiculo of(int sentados, int emPe, int cadeirantes, int necessidadesEspeciais) {
        if (sentados < 0 || emPe < 0 || cadeirantes < 0 || necessidadesEspeciais < 0) {
            throw new IllegalArgumentException("Capacidades não podem ser negativas");
        }

        if (sentados == 0 && emPe == 0 && cadeirantes == 0 && necessidadesEspeciais == 0) {
            throw new IllegalArgumentException("Veículo deve ter pelo menos uma capacidade positiva");
        }

        return new CapacidadeVeiculo(sentados, emPe, cadeirantes, necessidadesEspeciais);
    }

    public int getCapacidadeTotal() {
        return passageirosSentados + passageirosEmPe + cadeirantes + totalEspecialNecessidades;
    }

    public boolean podeAlocar(TipoPassageiro tipo, int quantidadeAtual) {
        return switch (tipo) {
            case SENTADO -> quantidadeAtual < passageirosSentados;
            case EM_PE -> quantidadeAtual < passageirosEmPe;
            case CADEIRANTE -> quantidadeAtual < cadeirantes;
            case NECESSIDADES_ESPECIAIS -> quantidadeAtual < totalEspecialNecessidades;
        };
    }
}