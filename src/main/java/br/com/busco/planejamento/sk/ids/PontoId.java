package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class PontoId extends DomainObjectId {

    public static final PontoId VAZIO = new PontoId();

    protected PontoId() {
        super("");
    }

    public PontoId(String uuid) {
        super(uuid);
    }

    public static PontoId randomId() {
        return randomId(PontoId.class);
    }

    public static PontoId fromString(@NonNull String uuid) {
        return fromString(uuid, PontoId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new PontoId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
