package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.ValueObject;
import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PUBLIC;

@Getter
@Embeddable
@EqualsAndHashCode(of = {"inicio", "fim"})
@NoArgsConstructor(access = PUBLIC, force = true)
@AllArgsConstructor(access = PRIVATE)
public class OrigemAgendamento implements ValueObject {
    @Enumerated(EnumType.STRING)
    @Column(name="origem_tipo")
    private TipoOrigem tipo;

    @Embedded
    @AttributeOverride(name = "uuid", column = @Column(name = "planejamento_id"))
    private PlanejamentoLoteId planejamentoId;

    public static OrigemAgendamento avulso() {
        return new OrigemAgendamento(TipoOrigem.AVULSO, null);
    }

    public static OrigemAgendamento lote(PlanejamentoLoteId id) {
        Objects.requireNonNull(id);
        return new OrigemAgendamento(TipoOrigem.LOTE, id);
    }

    public boolean ehDeLote() {
        return tipo == TipoOrigem.LOTE;
    }

    public boolean ehAvulso() {
        return tipo == TipoOrigem.AVULSO;
    }

}
