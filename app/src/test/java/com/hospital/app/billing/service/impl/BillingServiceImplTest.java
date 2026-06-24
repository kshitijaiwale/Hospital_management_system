package com.hospital.app.billing.service.impl;

import com.hospital.app.billing.dto.request.CreateInvoiceRequest;
import com.hospital.app.billing.dto.request.RecordPaymentRequest;
import com.hospital.app.billing.dto.response.InvoiceResponse;
import com.hospital.app.billing.dto.response.PaymentResponse;
import com.hospital.app.billing.entity.Invoice;
import com.hospital.app.billing.entity.Payment;
import com.hospital.app.billing.mapper.BillingMapper;
import com.hospital.app.billing.repository.InvoiceRepository;
import com.hospital.app.billing.repository.PaymentRepository;
import com.hospital.app.common.enums.InvoiceSourceType;
import com.hospital.app.common.enums.InvoiceStatus;
import com.hospital.app.common.enums.PaymentMode;
import com.hospital.app.common.enums.PaymentStatus;
import com.hospital.app.exception.BillingException;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private BillingMapper billingMapper;

    @InjectMocks private BillingServiceImpl billingService;

    @Test
    void generateInvoice_success() {
        UUID patientId = UUID.randomUUID();
        CreateInvoiceRequest req = CreateInvoiceRequest.builder()
                .patientId(patientId)
                .amount(new BigDecimal("1000"))
                .sourceType(InvoiceSourceType.APPOINTMENT)
                .build();

        Patient patient = new Patient();
        patient.setPatientId(patientId);

        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        Invoice savedInvoice = new Invoice();
        savedInvoice.setInvoiceId(UUID.randomUUID());
        savedInvoice.setAmount(new BigDecimal("1000"));
        savedInvoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.save(any())).thenReturn(savedInvoice);
        when(billingMapper.toInvoiceResponse(savedInvoice)).thenReturn(new InvoiceResponse());

        InvoiceResponse response = billingService.generateInvoice(req);

        assertNotNull(response);
        verify(invoiceRepository).save(any());
    }

    @Test
    void recordPayment_success_partial() {
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(new BigDecimal("400"))
                .paymentMode(PaymentMode.CASH)
                .build();

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setAmount(new BigDecimal("1000"));
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setPayments(new ArrayList<>());

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        
        InvoiceResponse mockResp = InvoiceResponse.builder()
                .remainingAmount(new BigDecimal("1000"))
                .paidAmount(BigDecimal.ZERO)
                .build();
        when(billingMapper.toInvoiceResponse(invoice)).thenReturn(mockResp);

        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("400"));
        payment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.save(any())).thenReturn(payment);
        when(billingMapper.toPaymentResponse(any())).thenReturn(new PaymentResponse());

        billingService.recordPayment(req);

        assertEquals(InvoiceStatus.PARTIALLY_PAID, invoice.getStatus());
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void recordPayment_exceedsBalance() {
        UUID invoiceId = UUID.randomUUID();
        RecordPaymentRequest req = RecordPaymentRequest.builder()
                .invoiceId(invoiceId)
                .amount(new BigDecimal("1500"))
                .build();

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(invoiceId);
        invoice.setAmount(new BigDecimal("1000"));
        invoice.setStatus(InvoiceStatus.PENDING);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        
        InvoiceResponse mockResp = InvoiceResponse.builder()
                .remainingAmount(new BigDecimal("1000"))
                .paidAmount(BigDecimal.ZERO)
                .build();
        when(billingMapper.toInvoiceResponse(invoice)).thenReturn(mockResp);

        BillingException ex = assertThrows(BillingException.class, () -> billingService.recordPayment(req));
        assertNotNull(ex);
    }
}
