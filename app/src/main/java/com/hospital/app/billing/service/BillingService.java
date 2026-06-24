package com.hospital.app.billing.service;

import com.hospital.app.billing.dto.request.CreateInvoiceRequest;
import com.hospital.app.billing.dto.request.RecordPaymentRequest;
import com.hospital.app.billing.dto.response.InvoiceResponse;
import com.hospital.app.billing.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface BillingService {
    InvoiceResponse generateInvoice(CreateInvoiceRequest request);
    PaymentResponse recordPayment(RecordPaymentRequest request);
    InvoiceResponse getInvoiceDetails(UUID invoiceId);
    List<InvoiceResponse> getPatientInvoices(UUID patientId);
}
