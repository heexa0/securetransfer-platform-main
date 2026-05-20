package com.securetransfer.platform.agency.controller;

import com.securetransfer.platform.agency.dto.AgencyOperationRequest;
import com.securetransfer.platform.agency.dto.AgencyOperationResponse;
import com.securetransfer.platform.agency.service.AgencyOperationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyOperationService agencyOperationService;

    @PostMapping("/operation")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCY')")
    public ResponseEntity<AgencyOperationResponse> createOperation(
            @Valid @RequestBody AgencyOperationRequest request) {
        return ResponseEntity.ok(agencyOperationService.createOperation(request));
    }

    @GetMapping("/{agencyId}/operations")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCY')")
    public ResponseEntity<List<AgencyOperationResponse>> getOperations(
            @PathVariable UUID agencyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(agencyOperationService.getOperationsByAgency(agencyId, page, size));
    }

    @GetMapping("/{agencyId}/daily-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENCY')")
    public ResponseEntity<List<AgencyOperationResponse>> getDailyReport(
            @PathVariable UUID agencyId) {
        return ResponseEntity.ok(agencyOperationService.getDailyReport(agencyId));
    }
}