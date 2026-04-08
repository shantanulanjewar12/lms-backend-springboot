package com.lms.enrollment.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentAccessStatusDto {
	private Boolean hasAccess;
	private LocalDateTime expiresAt;
	private String status;
	private Long remainingDays;
}
