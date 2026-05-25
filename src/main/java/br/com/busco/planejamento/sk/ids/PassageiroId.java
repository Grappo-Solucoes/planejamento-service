package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class PassageiroId extends DomainObjectId {

    public static final PassageiroId VAZIO = new PassageiroId();

    protected PassageiroId() {
        super("");
    }

    public PassageiroId(String uuid) {
        super(uuid);
    }

    public static PassageiroId randomId() {
        return randomId(PassageiroId.class);
    }

    public static PassageiroId fromString(@NonNull String uuid) {
        return fromString(uuid, PassageiroId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new PassageiroId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
