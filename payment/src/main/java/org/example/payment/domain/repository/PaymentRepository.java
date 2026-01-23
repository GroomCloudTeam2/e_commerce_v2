package org.example.payment.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groom.e_commerce.payment.domain.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

	Optional<Payment> findByOrderId(UUID orderId);
}
