package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class PlanejamentoLoteId extends DomainObjectId {

    public static final PlanejamentoLoteId VAZIO = new PlanejamentoLoteId();

    protected PlanejamentoLoteId() {
        super("");
    }

    public PlanejamentoLoteId(String uuid) {
        super(uuid);
    }

    public static PlanejamentoLoteId randomId() {
        return randomId(PlanejamentoLoteId.class);
    }

    public static PlanejamentoLoteId fromString(@NonNull String uuid) {
        return fromString(uuid, PlanejamentoLoteId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new PlanejamentoLoteId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
