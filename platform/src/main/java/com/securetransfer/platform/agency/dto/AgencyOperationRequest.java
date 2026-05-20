package com.securetransfer.platform.agency.dto;

import com.securetransfer.platform.agency.entity.AgencyOperation.OperationType;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyOperationRequest {

    @NotNull(message = "Agency ID is required")
    private UUID agencyId;

    @NotNull(message = "Operation type is required")
    private OperationType operationType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;

    private String description;
}