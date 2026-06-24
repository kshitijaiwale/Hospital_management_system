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
import com.hospital.app.billing.service.BillingService;
import com.hospital.app.common.enums.InvoiceStatus;
import com.hospital.app.common.enums.PaymentStatus;
import com.hospital.app.exception.BillingException;
import com.hospital.app.exception.ResourceNotFoundException;
import com.hospital.app.patient.entity.Patient;
import com.hospital.app.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final PatientRepository patientRepository;
    private final BillingMapper billingMapper;

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(CreateInvoiceRequest request) {
        log.info("Generating invoice for patient: {}", request.getPatientId());

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        Invoice invoice = Invoice.builder()
                .patient(patient)
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .amount(request.getAmount())
                .status(InvoiceStatus.PENDING)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        log.info("Generated invoice with ID: {}", savedInvoice.getInvoiceId());
        return billingMapper.toInvoiceResponse(savedInvoice);
    }

    @Override
    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        log.info("Recording payment for invoice: {}", request.getInvoiceId());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BillingException("Invoice is already fully paid.");
        }

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new BillingException("Cannot record payment for a cancelled invoice.");
        }

        InvoiceResponse invoiceDetails = billingMapper.toInvoiceResponse(invoice);
        BigDecimal remainingAmount = invoiceDetails.getRemainingAmount();

        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new BillingException("Payment amount (" + request.getAmount() + ") exceeds remaining balance (" + remainingAmount + ").");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentMode(request.getPaymentMode())
                .amount(request.getAmount())
                .status(PaymentStatus.SUCCESS)
                .paymentDatetime(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        invoice.getPayments().add(savedPayment);

        // Recalculate invoice status
        BigDecimal newPaidAmount = invoiceDetails.getPaidAmount().add(savedPayment.getAmount());
        if (newPaidAmount.compareTo(invoice.getAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        
        invoiceRepository.save(invoice);
        log.info("Recorded payment ID: {} for Invoice: {}", savedPayment.getPaymentId(), invoice.getInvoiceId());

        return billingMapper.toPaymentResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceDetails(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with ID: " + invoiceId));
        return billingMapper.toInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getPatientInvoices(UUID patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }
        
        return invoiceRepository.findByPatientPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(billingMapper::toInvoiceResponse)
                .collect(Collectors.toList());
    }
}
