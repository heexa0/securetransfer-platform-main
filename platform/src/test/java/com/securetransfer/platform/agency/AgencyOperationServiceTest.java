package com.securetransfer.platform.agency;

import com.securetransfer.platform.agency.dto.AgencyOperationRequest;
import com.securetransfer.platform.agency.dto.AgencyOperationResponse;
import com.securetransfer.platform.agency.entity.Agency;
import com.securetransfer.platform.agency.entity.AgencyOperation;
import com.securetransfer.platform.agency.repository.AgencyRepository;
import com.securetransfer.platform.agency.service.AgencyOperationService;
import com.securetransfer.platform.agency.service.CommissionCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgencyOperationServiceTest {

    @Mock
    private AgencyRepository agencyRepository;

    @Mock
    private CommissionCalculationService commissionService;

    @InjectMocks
    private AgencyOperationService agencyOperationService;

    private Agency agency;
    private UUID agencyId;

    @BeforeEach
    void setUp() {
        agencyId = UUID.randomUUID();
        agency = Agency.builder()
                .id(agencyId)
                .code("AG001")
                .name("Agence Casablanca")
                .city("Casablanca")
                .balance(new BigDecimal("10000.00"))
                .commissionRate(new BigDecimal("0.0050"))
                .active(true)
                .build();
    }

    @Test
    void testCreateDepositOperation() {
        AgencyOperationRequest request = AgencyOperationRequest.builder()
                .agencyId(agencyId)
                .operationType(AgencyOperation.OperationType.DEPOSIT)
                .amount(new BigDecimal("500.00"))
                .description("Test deposit")
                .build();

        when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(commissionService.calculateCommission(any(), any()))
                .thenReturn(new BigDecimal("2.5000"));
        when(agencyRepository.save(any())).thenReturn(agency);

        AgencyOperationResponse response = agencyOperationService.createOperation(request);

        assertNotNull(response);
        assertEquals(AgencyOperation.OperationStatus.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("2.5000"), response.getCommission());
        assertNotNull(response.getValidationCode());
        assertTrue(response.getValidationCode().startsWith("VAL-"));
    }

    @Test
    void testCreateWithdrawalInsufficientBalance() {
        AgencyOperationRequest request = AgencyOperationRequest.builder()
                .agencyId(agencyId)
                .operationType(AgencyOperation.OperationType.WITHDRAWAL)
                .amount(new BigDecimal("99999.00"))
                .description("Test withdrawal")
                .build();

        when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));
        when(commissionService.calculateCommission(any(), any()))
                .thenReturn(new BigDecimal("499.9950"));

        assertThrows(RuntimeException.class,
                () -> agencyOperationService.createOperation(request));
    }

    @Test
    void testAgencyNotFound() {
        AgencyOperationRequest request = AgencyOperationRequest.builder()
                .agencyId(UUID.randomUUID())
                .operationType(AgencyOperation.OperationType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .build();

        when(agencyRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> agencyOperationService.createOperation(request));
    }

    @Test
    void testInactiveAgency() {
        agency.setActive(false);

        AgencyOperationRequest request = AgencyOperationRequest.builder()
                .agencyId(agencyId)
                .operationType(AgencyOperation.OperationType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .build();

        when(agencyRepository.findById(agencyId)).thenReturn(Optional.of(agency));

        assertThrows(RuntimeException.class,
                () -> agencyOperationService.createOperation(request));
    }
}