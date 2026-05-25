package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ids.PlanejamentoLoteId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OrigemAgendamento - Testes Unitários")
class OrigemAgendamentoTest {

    @Test
    @DisplayName("Deve criar origem avulsa")
    void deveCriarOrigemAvulsa() {
        var origem = OrigemAgendamento.avulso();

        assertThat(origem.ehAvulso()).isTrue();
        assertThat(origem.ehDeLote()).isFalse();
        assertThat(origem.getPlanejamentoId()).isNull();
        assertThat(origem.getTipo()).isEqualTo(TipoOrigem.AVULSO);
    }

    @Test
    @DisplayName("Deve criar origem de lote com ID válido")
    void deveCriarOrigemLote() {
        var planejamentoId = PlanejamentoLoteId.randomId();
        var origem = OrigemAgendamento.lote(planejamentoId);

        assertThat(origem.ehDeLote()).isTrue();
        assertThat(origem.ehAvulso()).isFalse();
        assertThat(origem.getPlanejamentoId()).isEqualTo(planejamentoId);
        assertThat(origem.getTipo()).isEqualTo(TipoOrigem.LOTE);
    }

    @Test
    @DisplayName("Não deve criar origem de lote com ID nulo")
    void naoDeveCriarOrigemLoteComIdNulo() {
        assertThatThrownBy(() -> OrigemAgendamento.lote(null))
                .isInstanceOf(NullPointerException.class);
    }
}