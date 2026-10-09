package br.com.busco.planejamento.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Status - máquina de estados do planejamento (paridade com o monólito)")
class StatusTest {

    @Test
    @DisplayName("EM_ANALISE planeja para PLANEJADA")
    void emAnalisePlaneja() {
        assertThat(Status.EM_ANALISE.planejar()).isEqualTo(Status.PLANEJADA);
    }

    @Test
    @DisplayName("EM_ANALISE cancela para CANCELADA")
    void emAnaliseCancela() {
        assertThat(Status.EM_ANALISE.cancelar()).isEqualTo(Status.CANCELADA);
    }

    @Test
    @DisplayName("PLANEJADA inicia para INICIADA e cancela para CANCELADA")
    void planejadaTransicoes() {
        assertThat(Status.PLANEJADA.iniciar()).isEqualTo(Status.INICIADA);
        assertThat(Status.PLANEJADA.cancelar()).isEqualTo(Status.CANCELADA);
    }

    @Test
    @DisplayName("PLANEJADA não finaliza antes de iniciar (preserva estado)")
    void planejadaNaoFinaliza() {
        assertThat(Status.PLANEJADA.finalizar()).isEqualTo(Status.PLANEJADA);
    }

    @Test
    @DisplayName("INICIADA finaliza para FINALIZADA e não pode ser cancelada")
    void iniciadaTransicoes() {
        assertThat(Status.INICIADA.finalizar()).isEqualTo(Status.FINALIZADA);
        assertThat(Status.INICIADA.cancelar()).isEqualTo(Status.INICIADA);
    }

    @Test
    @DisplayName("FINALIZADA e CANCELADA são estados terminais (preservam estado)")
    void terminaisPreservamEstado() {
        assertThat(Status.FINALIZADA.cancelar()).isEqualTo(Status.FINALIZADA);
        assertThat(Status.CANCELADA.cancelar()).isEqualTo(Status.CANCELADA);
        assertThat(Status.CANCELADA.iniciar()).isEqualTo(Status.CANCELADA);
    }

    @Test
    @DisplayName("Transições sem equivalente no monólito lançam UnsupportedOperationException")
    void transicoesInvalidasLancamExcecao() {
        assertThatThrownBy(() -> Status.EM_ANALISE.iniciar())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> Status.EM_ANALISE.finalizar())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> Status.PLANEJADA.planejar())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Não existem status RASCUNHO, ATIVO ou SUSPENSO")
    void semStatusInventados() {
        assertThat(Status.values())
                .containsExactlyInAnyOrder(
                        Status.EM_ANALISE,
                        Status.PLANEJADA,
                        Status.INICIADA,
                        Status.FINALIZADA,
                        Status.CANCELADA);
    }
}
