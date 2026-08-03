package com.example.english_app.listener;

import com.example.english_app.service.subsription.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.KeyExpirationEventMessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentExpirationListener extends KeyExpirationEventMessageListener {

    private static final String PAYMENT_TIMEOUT_PREFIX = "payment_timeout:";
    
    private final PaymentService paymentService;

    public PaymentExpirationListener(RedisMessageListenerContainer listenerContainer, PaymentService paymentService) {
        super(listenerContainer);
        this.paymentService = paymentService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        
        if (expiredKey.startsWith(PAYMENT_TIMEOUT_PREFIX)) {
            String vnpTxnRef = expiredKey.substring(PAYMENT_TIMEOUT_PREFIX.length());
            log.info("Phát hiện giao dịch VNPAY hết hạn (15 phút). Hủy vnpTxnRef = {}", vnpTxnRef);
            
            try {
                paymentService.cancelPayment(vnpTxnRef);
            } catch (Exception e) {
                log.error("Lỗi khi xử lý hủy giao dịch tự động cho vnpTxnRef = {}", vnpTxnRef, e);
            }
        }
    }
}
