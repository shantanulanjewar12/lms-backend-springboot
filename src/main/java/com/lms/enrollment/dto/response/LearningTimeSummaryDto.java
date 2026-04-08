package com.lms.enrollment.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningTimeSummaryDto {
	private Long totalTimeSpentSeconds;
	private Long todayTimeSpentSeconds;
	private Long weeklyTimeSpentSeconds;
	private String formattedTotalTime;
	private String formattedTodayTime;
	private String formattedWeeklyTime;
	private List<DailyLearningTimeDto> dailyLearningTimes;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DailyLearningTimeDto {
		private String date;
		private Long seconds;
		private String formattedTime;
	}
}
