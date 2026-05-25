package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class RotaId extends DomainObjectId {

    public static final RotaId VAZIO = new RotaId();

    protected RotaId() {
        super("");
    }

    public RotaId(String uuid) {
        super(uuid);
    }

    public static RotaId randomId() {
        return randomId(RotaId.class);
    }

    public static RotaId fromString(@NonNull String uuid) {
        return fromString(uuid, RotaId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new RotaId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
