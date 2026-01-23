package org.example.payment.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.e_commerce.payment.domain.entity.PaymentCancel;

public interface PaymentCancelRepository extends JpaRepository<PaymentCancel, UUID> {
}
