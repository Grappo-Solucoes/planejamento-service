package br.com.busco.planejamento.infra.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    @Query("""
            SELECT o FROM OutboxEvent o
            WHERE o.status = :status
              AND (o.nextAttemptAt IS NULL OR o.nextAttemptAt <= :agora)
            ORDER BY o.createdAt ASC, o.id ASC
            """)
    List<OutboxEvent> buscarPendentes(
            @Param("status") OutboxStatus status,
            @Param("agora") LocalDateTime agora,
            Pageable pageable
    );

    List<OutboxEvent> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
}
