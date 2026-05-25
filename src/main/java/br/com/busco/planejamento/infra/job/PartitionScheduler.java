package br.com.busco.planejamento.infra.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionScheduler {

    private final JdbcTemplate jdbcTemplate;

    /*
     Executa:
     - todo dia às 02:00
     - cria partições para os próximos 3 meses
    */
    @Scheduled(cron = "0 0 2 * * *")
    public void createFuturePartitions() {

        try {

            LocalDate now = LocalDate.now()
                    .withDayOfMonth(1);

            for (int i = 0; i < 3; i++) {

                LocalDate targetMonth = now.plusMonths(i);

                createPresencaPartition(targetMonth);
            }

            log.info("Partições criadas/verificadas com sucesso");

        } catch (Exception ex) {
            log.error("Erro ao criar partições", ex);
        }
    }

    private void createPresencaPartition(LocalDate month) {

        jdbcTemplate.queryForObject(
                "SELECT create_presenca_partition(?)",
                Void.class,
                Date.valueOf(month)
        );
    }
}