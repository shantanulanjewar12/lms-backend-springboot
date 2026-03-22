package com.lms.notification.dto.response;

import java.time.LocalDateTime;

import com.lms.notification.vo.BadgeType;

import lombok.Data;

@Data
public class BadgeResponseDto {
	private Long id;
	private Long studentId;
	private BadgeType badgeType;
	private String badgeLabel;
	private String badgeDescription;
	private LocalDateTime awardedAt;
}