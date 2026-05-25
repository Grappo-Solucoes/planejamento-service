package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.ValueObject;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.Set;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PUBLIC;

@Getter
@Embeddable
@EqualsAndHashCode(of = {"inicio", "fim"})
@NoArgsConstructor(access = PUBLIC, force = true)
@AllArgsConstructor(access = PRIVATE)
public class RecorrenciaSemanal implements ValueObject {

    private final Set<DayOfWeek> diasDaSemana;


    public boolean ocorreNoDia(DayOfWeek dia) {
        return diasDaSemana.contains(dia);
    }

    public static RecorrenciaSemanal of(Set<DayOfWeek> diasDaSemana) {
        if (diasDaSemana == null || diasDaSemana.isEmpty()) {
            throw new IllegalArgumentException("Deve existir ao menos um dia da semana");
        }
        return new RecorrenciaSemanal(diasDaSemana);
    }
}