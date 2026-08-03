package com.example.english_app.repository.subscription;

import com.example.english_app.entity.subscription.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByVnpTxnRef(String vnpTxnRef);
}
