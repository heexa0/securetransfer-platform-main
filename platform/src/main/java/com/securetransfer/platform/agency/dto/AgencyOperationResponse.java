package com.securetransfer.platform.agency.dto;

import com.securetransfer.platform.agency.entity.AgencyOperation.OperationType;
import com.securetransfer.platform.agency.entity.AgencyOperation.OperationStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyOperationResponse {

    private UUID id;
    private UUID agencyId;
    private String agencyName;
    private OperationType operationType;
    private BigDecimal amount;
    private BigDecimal commission;
    private String validationCode;
    private String description;
    private OperationStatus status;
    private Instant createdAt;
}