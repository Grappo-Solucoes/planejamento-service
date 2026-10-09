package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class PlanejamentoId extends DomainObjectId {

    protected PlanejamentoId() {
        super("");
    }

    public PlanejamentoId(String uuid) {
        super(uuid);
    }

    public static PlanejamentoId randomId() {
        return randomId(PlanejamentoId.class);
    }

    public static PlanejamentoId fromString(@NonNull String uuid) {
        return fromString(uuid, PlanejamentoId.class);
    }
}
