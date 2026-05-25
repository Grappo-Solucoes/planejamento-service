package br.com.busco.planejamento.sk.ddd;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static lombok.AccessLevel.PROTECTED;
import static org.springframework.data.util.ProxyUtils.getUserClass;

/**
 * Base class for entities.
 *
 * @param <ID> the entity ID type.
 */
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "tenantId", type = String.class)
)
@Filter(
        name = "tenantFilter",
        condition = "tenant_id = :tenantId"
)
@EntityListeners(TenantEntityListener.class)
@MappedSuperclass
@NoArgsConstructor(access = PROTECTED)
public abstract class AbstractEntity<ID extends DomainObjectId>
        implements IdentifiableDomainObject<ID>, ConcurrentDomainObject {

    @EmbeddedId
    @AttributeOverride(name = "uuid", column = @Column(name = "id"))
    private ID id;

    @Setter
    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "tenant_id"))
    private TenantId tenantId;

    @Version
    @Nullable
    private Long version;

    /**
     * Copy constructor
     *
     * @param source the entity to copy from.
     */
    protected AbstractEntity(@NonNull AbstractEntity<ID> source) {
        requireNonNull(source, "source must not be null");
        this.id = source.id;
        this.tenantId = source.tenantId;
    }

    /**
     * Constructor for creating new entities.
     *
     * @param id the ID to assign to the entity.
     */
    protected AbstractEntity(@NonNull ID id, TenantId tenantId) {
        this.id = requireNonNull(id, "id must not be null");
        this.tenantId = tenantId;
    }

    @NonNull
    @Override
    public ID getId() {
        return id;
    }

    @NonNull
    public TenantId getTenantId() {
        return tenantId;
    }

    @NonNull
    @Override
    public Long version() {
        return version;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (obj == null || !getClass().equals(getUserClass(obj)))
            return false;

        var other = (AbstractEntity<?>) obj;

        return id != null &&
                id.equals(other.id) &&
                Objects.equals(tenantId, other.tenantId);    }

    @Override
    public int hashCode() {
        return id == null ? super.hashCode() : id.hashCode();
    }

    @Override
    public String toString() {
        return format("%s[%s]", getClass().getSimpleName(), id);
    }

}