package com.hospital.app.billing.controller;

import com.hospital.app.billing.dto.request.CreateInvoiceRequest;
import com.hospital.app.billing.dto.response.InvoiceResponse;
import com.hospital.app.billing.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> generateInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        return new ResponseEntity<>(billingService.generateInvoice(request), HttpStatus.CREATED);
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoiceDetails(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(billingService.getInvoiceDetails(invoiceId));
    }

    @GetMapping("/patients/{patientId}/invoices")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<InvoiceResponse>> getPatientInvoices(@PathVariable UUID patientId) {
        return ResponseEntity.ok(billingService.getPatientInvoices(patientId));
    }
}
