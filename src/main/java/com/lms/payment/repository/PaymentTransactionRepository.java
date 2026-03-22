package com.lms.payment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lms.payment.entity.PaymentTransaction;
import com.lms.payment.vo.PaymentStatus;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
	List<PaymentTransaction> findByStudentId(Long studentId);

	Optional<PaymentTransaction> findByStripeSessionId(String sessionId);

	boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, PaymentStatus status);

	Page<PaymentTransaction> findAll(Pageable pageable);
}