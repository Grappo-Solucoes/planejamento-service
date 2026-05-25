package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class AlocacaoPassageiroId extends DomainObjectId {

    public static final AlocacaoPassageiroId VAZIO = new AlocacaoPassageiroId();

    protected AlocacaoPassageiroId() {
        super("");
    }

    public AlocacaoPassageiroId(String uuid) {
        super(uuid);
    }

    public static AlocacaoPassageiroId randomId() {
        return randomId(AlocacaoPassageiroId.class);
    }

    public static AlocacaoPassageiroId fromString(@NonNull String uuid) {
        return fromString(uuid, AlocacaoPassageiroId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new AlocacaoPassageiroId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
