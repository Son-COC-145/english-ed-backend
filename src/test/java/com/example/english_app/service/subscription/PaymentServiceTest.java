package com.example.english_app.service.subscription;

import com.example.english_app.config.VnPayConfig;
import com.example.english_app.config.VnPayUtil;
import com.example.english_app.dto.response.PaymentStatusResponse;
import com.example.english_app.entity.enums.PaymentStatus;
import com.example.english_app.entity.enums.PlanName;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.enums.SubscriptionStatus;
import com.example.english_app.entity.subscription.PaymentTransaction;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import com.example.english_app.entity.subscription.UserSubscription;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.subscription.PaymentTransactionRepository;
import com.example.english_app.repository.subscription.SubscriptionPlanRepository;
import com.example.english_app.repository.subscription.UserSubscriptionRepository;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private VnPayConfig vnPayConfig;

    @Mock
    private VnPayUtil vnPayUtil;

    @Mock
    private SubscriptionPlanRepository planRepository;

    @Mock
    private UserSubscriptionRepository userSubscriptionRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private User owner;
    private User otherUser;
    private User admin;
    private SubscriptionPlan plan;
    private UserSubscription subscription;
    private PaymentTransaction transaction;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("student@test.com")
                .role(Role.STUDENT)
                .build();

        otherUser = User.builder()
                .id(2L)
                .email("other@test.com")
                .role(Role.STUDENT)
                .build();

        admin = User.builder()
                .id(3L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();

        plan = SubscriptionPlan.builder()
                .id(10L)
                .name(PlanName.PREMIUM)
                .price(BigDecimal.valueOf(99000))
                .durationDays(30)
                .build();

        subscription = UserSubscription.builder()
                .id(100L)
                .user(owner)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.of(2026, 9, 17, 10, 0))
                .endDate(LocalDateTime.of(2026, 10, 17, 10, 0))
                .build();

        transaction = PaymentTransaction.builder()
                .id(1000L)
                .user(owner)
                .subscription(subscription)
                .amount(BigDecimal.valueOf(99000))
                .orderInfo("Thanh toan don hang 1726557891234")
                .vnpTxnRef("1726557891234")
                .vnpTransactionNo("14682390")
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.of(2026, 9, 17, 10, 0))
                .build();
    }

    @Test
    @DisplayName("Lấy trạng thái thanh toán thành công bởi chính chủ sở hữu")
    void getPaymentStatus_asOwner_success() {
        when(paymentTransactionRepository.findByVnpTxnRef("1726557891234"))
                .thenReturn(Optional.of(transaction));
        when(userRepository.findByEmail("student@test.com"))
                .thenReturn(Optional.of(owner));

        PaymentStatusResponse response = paymentService.getPaymentStatus("1726557891234", "student@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getTxnRef()).isEqualTo("1726557891234");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPlanName()).isEqualTo(PlanName.PREMIUM);
        assertThat(response.getPlanDurationDays()).isEqualTo(30);
        assertThat(response.getAmount()).isEqualTo(BigDecimal.valueOf(99000));
        assertThat(response.getVnpTransactionNo()).isEqualTo("14682390");
    }

    @Test
    @DisplayName("ADMIN có thể xem trạng thái thanh toán của bất kỳ người dùng nào")
    void getPaymentStatus_asAdmin_success() {
        when(paymentTransactionRepository.findByVnpTxnRef("1726557891234"))
                .thenReturn(Optional.of(transaction));
        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.of(admin));

        PaymentStatusResponse response = paymentService.getPaymentStatus("1726557891234", "admin@test.com");

        assertThat(response).isNotNull();
        assertThat(response.getTxnRef()).isEqualTo("1726557891234");
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Ném lỗi ACCESS_DENIED khi user khác cố truy cập giao dịch không phải của mình")
    void getPaymentStatus_asOtherUser_throwsAccessDenied() {
        when(paymentTransactionRepository.findByVnpTxnRef("1726557891234"))
                .thenReturn(Optional.of(transaction));
        when(userRepository.findByEmail("other@test.com"))
                .thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> paymentService.getPaymentStatus("1726557891234", "other@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED);
    }

    @Test
    @DisplayName("Ném lỗi PAYMENT_TRANSACTION_NOT_FOUND khi mã giao dịch không tồn tại")
    void getPaymentStatus_transactionNotFound_throwsAppException() {
        when(paymentTransactionRepository.findByVnpTxnRef("invalid_ref"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("invalid_ref", "student@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND);
    }

    @Test
    @DisplayName("Ném lỗi USER_NOT_FOUND khi email người dùng không tồn tại")
    void getPaymentStatus_userNotFound_throwsAppException() {
        when(paymentTransactionRepository.findByVnpTxnRef("1726557891234"))
                .thenReturn(Optional.of(transaction));
        when(userRepository.findByEmail("notfound@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("1726557891234", "notfound@test.com"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }
}
