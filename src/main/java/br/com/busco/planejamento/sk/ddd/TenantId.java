package br.com.busco.planejamento.sk.ddd;

import lombok.NonNull;

import java.util.UUID;


public class TenantId extends DomainObjectId {

    protected TenantId() {
        super("");
    }

    public TenantId(String uuid) {
        super(uuid);
    }

    public static TenantId randomId() {
        return randomId(TenantId.class);
    }

    public static TenantId fromString(@NonNull String uuid) {
        return fromString(uuid, TenantId.class);
    }

    public static TenantId fromUUID(@NonNull UUID uuid) {
        return fromString(uuid.toString(), TenantId.class);
    }

}