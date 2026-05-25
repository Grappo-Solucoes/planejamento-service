package br.com.busco.planejamento.domain;

import br.com.busco.planejamento.sk.ddd.ValueObject;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PUBLIC;

@Getter
@Embeddable
@EqualsAndHashCode(of = {"inicio", "fim"})
@NoArgsConstructor(access = PUBLIC, force = true)
@AllArgsConstructor(access = PRIVATE)
public class Periodo implements ValueObject {
    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    public static Periodo of(LocalDateTime inicio, LocalDateTime fim) {
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Data final não pode ser anterior ao início");
        }

        return new Periodo(inicio, fim);
    }
}
