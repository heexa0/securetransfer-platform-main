package com.securetransfer.platform.agency.service;

import com.securetransfer.platform.agency.entity.Agency;
import com.securetransfer.platform.agency.entity.AgencyOperation;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommissionCalculationService {

    public BigDecimal calculateCommission(BigDecimal amount, BigDecimal commissionRate) {
        return amount.multiply(commissionRate)
                .setScale(4, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalCommissions(List<AgencyOperation> operations) {
        return operations.stream()
                .filter(op -> op.getStatus() == AgencyOperation.OperationStatus.COMPLETED)
                .map(AgencyOperation::getCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<AgencyOperation.OperationType, BigDecimal> groupCommissionsByType(
            List<AgencyOperation> operations) {
        return operations.stream()
                .filter(op -> op.getStatus() == AgencyOperation.OperationStatus.COMPLETED)
                .collect(Collectors.groupingBy(
                        AgencyOperation::getOperationType,
                        Collectors.reducing(BigDecimal.ZERO,
                                AgencyOperation::getCommission,
                                BigDecimal::add)));
    }

    public BigDecimal calculateDailyCommission(Agency agency, List<AgencyOperation> dailyOps) {
        return dailyOps.stream()
                .filter(op -> op.getStatus() == AgencyOperation.OperationStatus.COMPLETED)
                .map(op -> calculateCommission(op.getAmount(), agency.getCommissionRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}