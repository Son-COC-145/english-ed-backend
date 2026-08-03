package com.example.english_app.service.subsription;

import com.example.english_app.config.VnPayConfig;
import com.example.english_app.config.VnPayUtil;
import com.example.english_app.entity.enums.PaymentStatus;
import com.example.english_app.entity.enums.SubscriptionStatus;
import com.example.english_app.entity.subscription.PaymentTransaction;
import com.example.english_app.entity.subscription.SubscriptionPlan;
import com.example.english_app.entity.subscription.UserSubscription;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.subscription.PaymentTransactionRepository;
import com.example.english_app.repository.subscription.SubscriptionPlanRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.subscription.UserSubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final VnPayUtil vnPayUtil;
    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String PAYMENT_TIMEOUT_PREFIX = "payment_timeout:";

    @Transactional
    public String createPaymentUrl(Long planId, String email, HttpServletRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());
        
        // 1. Tạo UserSubscription (PENDING_PAYMENT)
        UserSubscription subscription = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(plan.getDurationDays())) 
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();
        userSubscriptionRepository.save(subscription);

        // 2. Tạo PaymentTransaction (PENDING)
        PaymentTransaction transaction = PaymentTransaction.builder()
                .user(user)
                .subscription(subscription)
                .amount(plan.getPrice())
                .vnpTxnRef(vnp_TxnRef)
                .orderInfo("Thanh toan don hang " + vnp_TxnRef)
                .status(PaymentStatus.PENDING)
                .build();
        paymentTransactionRepository.save(transaction);

        // 3. Xây dựng URL VNPay
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        String ipAddr = vnPayUtil.getIpAddress(request);
        
        long amountInVnd = plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode().trim());
        vnp_Params.put("vnp_Amount", String.valueOf(amountInVnd));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", transaction.getOrderInfo());
        vnp_Params.put("vnp_OrderType", orderType);
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl().trim());
        vnp_Params.put("vnp_IpAddr", ipAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                try {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                } catch (Exception e) {
                    log.error("Lỗi encode VNPay: ", e);
                }
            }
        }
        
        String queryUrl = query.toString();
        String vnp_SecureHash = vnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        // Lưu key vào Redis với thời gian hết hạn 15 phút
        redisTemplate.opsForValue().set(PAYMENT_TIMEOUT_PREFIX + vnp_TxnRef, "pending", 15, TimeUnit.MINUTES);

        return vnPayConfig.getUrl() + "?" + queryUrl;
    }

    @Transactional
    public Map<String, String> processVnpayIpn(Map<String, String> params) {
        Map<String, String> result = new HashMap<>();
        try {
            Map<String, String> fields = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String fieldName = entry.getKey();
                String fieldValue = entry.getValue();
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    fields.put(fieldName, fieldValue);
                }
            }

            String vnp_SecureHash = fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String signValue = vnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            
            if (signValue.equals(vnp_SecureHash)) {
                String vnp_TxnRef = fields.get("vnp_TxnRef");
                String vnp_ResponseCode = fields.get("vnp_ResponseCode");
                String vnp_TransactionNo = fields.get("vnp_TransactionNo");

                Optional<PaymentTransaction> transactionOpt = paymentTransactionRepository.findByVnpTxnRef(vnp_TxnRef);
                if (transactionOpt.isPresent()) {
                    PaymentTransaction transaction = transactionOpt.get();
                    if (transaction.getStatus() == PaymentStatus.PENDING) {
                        if ("00".equals(vnp_ResponseCode)) {
                            // Thành công
                            transaction.setStatus(PaymentStatus.SUCCESS);
                            transaction.setVnpTransactionNo(vnp_TransactionNo);
                            
                            UserSubscription subscription = transaction.getSubscription();
                            subscription.setStatus(SubscriptionStatus.ACTIVE);
                            subscription.setStartDate(LocalDateTime.now());
                            subscription.setEndDate(LocalDateTime.now().plusDays(subscription.getPlan().getDurationDays()));
                            
                            userSubscriptionRepository.save(subscription);
                        } else {
                            // Thất bại
                            transaction.setStatus(PaymentStatus.FAILED);
                        }
                        paymentTransactionRepository.save(transaction);
                        
                        // Xoá key hết hạn trong Redis vì đã xử lý xong
                        redisTemplate.delete(PAYMENT_TIMEOUT_PREFIX + vnp_TxnRef);
                        
                        result.put("RspCode", "00");
                        result.put("Message", "Confirm Success");
                    } else {
                        result.put("RspCode", "02");
                        result.put("Message", "Order already confirmed");
                    }
                } else {
                    result.put("RspCode", "01");
                    result.put("Message", "Order not found");
                }
            } else {
                result.put("RspCode", "97");
                result.put("Message", "Invalid Checksum");
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý IPN VNPay", e);
            result.put("RspCode", "99");
            result.put("Message", "Unknown error");
        }
        return result;
    }

    @Transactional
    public void cancelPayment(String vnpTxnRef) {
        paymentTransactionRepository.findByVnpTxnRef(vnpTxnRef).ifPresent(transaction -> {
            if (transaction.getStatus() == PaymentStatus.PENDING) {
                transaction.setStatus(PaymentStatus.FAILED);
                paymentTransactionRepository.save(transaction);
                
                UserSubscription subscription = transaction.getSubscription();
                if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT) {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    userSubscriptionRepository.save(subscription);
                }
                log.info("Đã hủy tự động giao dịch vnpTxnRef = {}", vnpTxnRef);
            }
        });
    }
}
