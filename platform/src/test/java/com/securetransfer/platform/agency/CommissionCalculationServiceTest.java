package com.securetransfer.platform.agency;

import com.securetransfer.platform.agency.entity.Agency;
import com.securetransfer.platform.agency.entity.AgencyOperation;
import com.securetransfer.platform.agency.service.CommissionCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CommissionCalculationServiceTest {

    private CommissionCalculationService service;

    @BeforeEach
    void setUp() {
        service = new CommissionCalculationService();
    }

    @Test
    void testCalculateCommission() {
        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal rate = new BigDecimal("0.0050");
        BigDecimal result = service.calculateCommission(amount, rate);
        assertEquals(new BigDecimal("5.0000"), result);
    }

    @Test
    void testCalculateTotalCommissions() {
        AgencyOperation op1 = new AgencyOperation();
        op1.setCommission(new BigDecimal("5.0000"));
        op1.setStatus(AgencyOperation.OperationStatus.COMPLETED);

        AgencyOperation op2 = new AgencyOperation();
        op2.setCommission(new BigDecimal("3.0000"));
        op2.setStatus(AgencyOperation.OperationStatus.COMPLETED);

        AgencyOperation op3 = new AgencyOperation();
        op3.setCommission(new BigDecimal("2.0000"));
        op3.setStatus(AgencyOperation.OperationStatus.FAILED);

        BigDecimal total = service.calculateTotalCommissions(List.of(op1, op2, op3));
        assertEquals(new BigDecimal("8.0000"), total);
    }

    @Test
    void testCalculateDailyCommission() {
        Agency agency = new Agency();
        agency.setCommissionRate(new BigDecimal("0.0050"));

        AgencyOperation op1 = new AgencyOperation();
        op1.setAmount(new BigDecimal("1000.00"));
        op1.setStatus(AgencyOperation.OperationStatus.COMPLETED);

        AgencyOperation op2 = new AgencyOperation();
        op2.setAmount(new BigDecimal("2000.00"));
        op2.setStatus(AgencyOperation.OperationStatus.COMPLETED);

        BigDecimal daily = service.calculateDailyCommission(agency, List.of(op1, op2));
        assertEquals(new BigDecimal("15.0000"), daily);
    }
}