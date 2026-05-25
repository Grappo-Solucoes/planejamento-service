package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class MotoristaId extends DomainObjectId {

    public static final MotoristaId VAZIO = new MotoristaId();

    protected MotoristaId() {
        super("");
    }

    public MotoristaId(String uuid) {
        super(uuid);
    }

    public static MotoristaId randomId() {
        return randomId(MotoristaId.class);
    }

    public static MotoristaId fromString(@NonNull String uuid) {
        return fromString(uuid, MotoristaId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new MotoristaId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
