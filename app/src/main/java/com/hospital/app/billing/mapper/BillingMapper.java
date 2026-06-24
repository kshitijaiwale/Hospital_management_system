package com.hospital.app.billing.mapper;

import com.hospital.app.billing.dto.response.InvoiceResponse;
import com.hospital.app.billing.dto.response.PaymentResponse;
import com.hospital.app.billing.entity.Invoice;
import com.hospital.app.billing.entity.Payment;
import com.hospital.app.common.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BillingMapper {

    public PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) return null;
        
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getInvoiceId() : null)
                .paymentMode(payment.getPaymentMode())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentDatetime(payment.getPaymentDatetime())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        if (invoice == null) return null;

        List<Payment> payments = invoice.getPayments() != null ? invoice.getPayments() : Collections.emptyList();
        
        BigDecimal paidAmount = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalAmount = invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = totalAmount.subtract(paidAmount);
        
        // Prevent negative remaining amount if overpaid
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return InvoiceResponse.builder()
                .invoiceId(invoice.getInvoiceId())
                .patientId(invoice.getPatient() != null ? invoice.getPatient().getPatientId() : null)
                .patientName(invoice.getPatient() != null && invoice.getPatient().getUser() != null ? invoice.getPatient().getUser().getName() : null)
                .sourceType(invoice.getSourceType())
                .sourceId(invoice.getSourceId())
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .remainingAmount(remainingAmount)
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .payments(payments.stream().map(this::toPaymentResponse).collect(Collectors.toList()))
                .build();
    }
}
