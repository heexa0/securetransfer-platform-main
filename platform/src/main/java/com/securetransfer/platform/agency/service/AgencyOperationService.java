package com.securetransfer.platform.agency.service;
import java.math.BigDecimal;
import com.securetransfer.platform.agency.dto.AgencyOperationRequest;
import com.securetransfer.platform.agency.dto.AgencyOperationResponse;
import com.securetransfer.platform.agency.entity.Agency;
import com.securetransfer.platform.agency.entity.AgencyOperation;
import com.securetransfer.platform.agency.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgencyOperationService {

    private final AgencyRepository agencyRepository;
    private final CommissionCalculationService commissionService;

    @Transactional
    public AgencyOperationResponse createOperation(AgencyOperationRequest request) {
        Agency agency = agencyRepository.findById(request.getAgencyId())
                .orElseThrow(() -> new RuntimeException("Agency not found"));

        if (!agency.isActive()) {
            throw new RuntimeException("Agency is not active");
        }

        String validationCode = "VAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        BigDecimal commission = commissionService.calculateCommission(
                request.getAmount(), agency.getCommissionRate());

        AgencyOperation operation = AgencyOperation.builder()
                .agency(agency)
                .operationType(request.getOperationType())
                .amount(request.getAmount())
                .commission(commission)
                .validationCode(validationCode)
                .description(request.getDescription())
                .status(AgencyOperation.OperationStatus.PENDING)
                .build();

        if (request.getOperationType() == AgencyOperation.OperationType.WITHDRAWAL) {
            if (agency.getBalance().compareTo(request.getAmount()) < 0) {
                throw new RuntimeException("Insufficient balance");
            }
            agency.setBalance(agency.getBalance().subtract(request.getAmount()));
        } else if (request.getOperationType() == AgencyOperation.OperationType.DEPOSIT) {
            agency.setBalance(agency.getBalance().add(request.getAmount()));
        }

        operation.setStatus(AgencyOperation.OperationStatus.COMPLETED);
        agencyRepository.save(agency);

        return mapToResponse(operation);
    }

    public List<AgencyOperationResponse> getDailyReport(UUID agencyId) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant endOfDay = startOfDay.plus(1, ChronoUnit.DAYS);

        return agencyRepository.findOperationsByAgencyIdAndDateRange(agencyId, startOfDay, endOfDay)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<AgencyOperationResponse> getOperationsByAgency(UUID agencyId, int page, int size) {
        return agencyRepository.findOperationsByAgencyId(agencyId, PageRequest.of(page, size))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AgencyOperationResponse mapToResponse(AgencyOperation op) {
        return AgencyOperationResponse.builder()
                .id(op.getId())
                .agencyId(op.getAgency().getId())
                .agencyName(op.getAgency().getName())
                .operationType(op.getOperationType())
                .amount(op.getAmount())
                .commission(op.getCommission())
                .validationCode(op.getValidationCode())
                .description(op.getDescription())
                .status(op.getStatus())
                .createdAt(op.getCreatedAt())
                .build();
    }
}