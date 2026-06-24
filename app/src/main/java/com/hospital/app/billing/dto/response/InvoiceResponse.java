package com.hospital.app.billing.dto.response;

import com.hospital.app.common.enums.InvoiceSourceType;
import com.hospital.app.common.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private UUID invoiceId;
    private UUID patientId;
    private String patientName;
    private InvoiceSourceType sourceType;
    private UUID sourceId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
    private List<PaymentResponse> payments;
}
