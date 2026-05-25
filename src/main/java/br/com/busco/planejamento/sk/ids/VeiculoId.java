package br.com.busco.planejamento.sk.ids;

import br.com.busco.planejamento.sk.ddd.DomainObjectId;
import lombok.NonNull;

public class VeiculoId extends DomainObjectId {

    public static final VeiculoId VAZIO = new VeiculoId();

    protected VeiculoId() {
        super("");
    }

    public VeiculoId(String uuid) {
        super(uuid);
    }

    public static VeiculoId randomId() {
        return randomId(VeiculoId.class);
    }

    public static VeiculoId fromString(@NonNull String uuid) {
        return fromString(uuid, VeiculoId.class);
    }

    public boolean isEmpty() {
        return this.equals(VAZIO) || this.equals(new VeiculoId());
    }

    public boolean isPresent() {
        return !isEmpty();
    }
}
