package com.hospital.app.billing.dto.response;

import com.hospital.app.common.enums.PaymentMode;
import com.hospital.app.common.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private UUID paymentId;
    private UUID invoiceId;
    private PaymentMode paymentMode;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paymentDatetime;
    private LocalDateTime createdAt;
}
