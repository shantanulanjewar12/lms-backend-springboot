package com.lms.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lms.notification.entity.StudentBadge;
import com.lms.notification.vo.BadgeType;

public interface StudentBadgeRepository extends JpaRepository<StudentBadge, Long> {
	List<StudentBadge> findByStudentId(Long studentId);

	long countByStudentId(Long studentId);
	
	boolean existsByStudentIdAndBadgeType(Long studentId, BadgeType badgeType);
}