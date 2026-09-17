package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.PaymentStatus;
import com.example.english_app.entity.enums.PlanName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết và trạng thái giao dịch thanh toán")
public class PaymentStatusResponse {

    @Schema(description = "Mã tham chiếu giao dịch (vnp_TxnRef)", example = "1726557891234")
    private String txnRef;

    @Schema(description = "Trạng thái thanh toán (PENDING, SUCCESS, FAILED)", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "Tên gói cước", example = "PREMIUM")
    private PlanName planName;

    @Schema(description = "Thời hạn gói cước (ngày)", example = "30")
    private Integer planDurationDays;

    @Schema(description = "Số tiền thanh toán (VNĐ)", example = "99000")
    private BigDecimal amount;

    @Schema(description = "Nội dung đơn hàng", example = "Thanh toan don hang 1726557891234")
    private String orderInfo;

    @Schema(description = "Mã giao dịch tại hệ thống VNPay", example = "14682390")
    private String vnpTransactionNo;

    @Schema(description = "Thời gian tạo giao dịch")
    private LocalDateTime createdAt;

    @Schema(description = "Ngày bắt đầu gói cước (khi thanh toán thành công)")
    private LocalDateTime subscriptionStartDate;

    @Schema(description = "Ngày hết hạn gói cước (khi thanh toán thành công)")
    private LocalDateTime subscriptionEndDate;
}
