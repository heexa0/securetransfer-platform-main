package com.securetransfer.platform.agency.repository;
import java.math.BigDecimal;
import com.securetransfer.platform.agency.entity.Agency;
import com.securetransfer.platform.agency.entity.AgencyOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByCode(String code);

    List<Agency> findByActiveTrue();

    @Query("SELECT o FROM AgencyOperation o WHERE o.agency.id = :agencyId ORDER BY o.createdAt DESC")
    List<AgencyOperation> findOperationsByAgencyId(@Param("agencyId") UUID agencyId, Pageable pageable);

    @Query("SELECT o FROM AgencyOperation o WHERE o.agency.id = :agencyId AND o.createdAt BETWEEN :start AND :end")
    List<AgencyOperation> findOperationsByAgencyIdAndDateRange(
            @Param("agencyId") UUID agencyId,
            @Param("start") Instant start,
            @Param("end") Instant end);

    @Query("SELECT SUM(o.amount) FROM AgencyOperation o WHERE o.agency.id = :agencyId AND o.status = 'COMPLETED'")
    BigDecimal getTotalAmountByAgencyId(@Param("agencyId") UUID agencyId);
}