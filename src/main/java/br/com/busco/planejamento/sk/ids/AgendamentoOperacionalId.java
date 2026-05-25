package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class AgendamentoOperacionalId extends DomainObjectId {

    public static final AgendamentoOperacionalId VAZIO = new AgendamentoOperacionalId();

    protected AgendamentoOperacionalId() {
        super("");
    }

    public AgendamentoOperacionalId(String uuid) {
        super(uuid);
    }

    public static AgendamentoOperacionalId randomId() {
        return randomId(AgendamentoOperacionalId.class);
    }

    public static AgendamentoOperacionalId fromString(@NonNull String uuid) {
        return fromString(uuid, AgendamentoOperacionalId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new AgendamentoOperacionalId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
