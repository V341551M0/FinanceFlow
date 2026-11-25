package com.financeflow.space;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SpaceTransferRepository extends JpaRepository<SpaceTransfer, Long> {

    @EntityGraph(attributePaths = {"sourceSpace", "destinationSpace"})
    List<SpaceTransfer> findTop50ByUserIdOrderByTransferDateDescIdDesc(Long userId);

    @Query("""
        SELECT COALESCE(SUM(st.amount), 0)
        FROM SpaceTransfer st
        WHERE st.user.id = :userId
          AND st.destinationSpace.id = :spaceId
          AND st.transferDate BETWEEN :from AND :to
    """)
    BigDecimal sumIncomingForSpace(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("""
        SELECT COALESCE(SUM(st.amount), 0)
        FROM SpaceTransfer st
        WHERE st.user.id = :userId
          AND st.sourceSpace.id = :spaceId
          AND st.transferDate BETWEEN :from AND :to
    """)
    BigDecimal sumOutgoingForSpace(
            @Param("userId") Long userId,
            @Param("spaceId") Long spaceId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
