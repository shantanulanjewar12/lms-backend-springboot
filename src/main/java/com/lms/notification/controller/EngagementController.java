package com.lms.notification.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lms.notification.dto.response.BadgeResponseDto;
import com.lms.notification.dto.response.LeaderboardResponseDto;
import com.lms.notification.entity.LearningStreak;
import com.lms.notification.repository.LearningStreakRepository;
import com.lms.notification.repository.StudentBadgeRepository;
import com.lms.notification.service.BadgeService;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.shared.dto.ApiResponse;
import com.lms.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/engagement")
@RequiredArgsConstructor
public class EngagementController {

	private final BadgeService badgeService;
	private final LearningStreakRepository streakRepository;
	private final StudentBadgeRepository badgeRepository;
	private final EnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;

	@GetMapping("/badges/{studentId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<List<BadgeResponseDto>>> getBadges(@PathVariable Long studentId) {
		return ResponseEntity.ok(ApiResponse.success("Badges fetched", badgeService.getStudentBadges(studentId)));
	}

	@GetMapping("/streak/{studentId}")
	@PreAuthorize("hasRole('STUDENT')")
	public ResponseEntity<ApiResponse<LearningStreak>> getStreak(@PathVariable Long studentId) {
		LearningStreak streak = streakRepository.findByStudentId(studentId)
				.orElse(LearningStreak.builder().studentId(studentId).build());
		return ResponseEntity.ok(ApiResponse.success("Streak data", streak));
	}

	@GetMapping("/leaderboard")
	public ResponseEntity<ApiResponse<List<LeaderboardResponseDto>>> getLeaderboard() {
		List<LearningStreak> streaks = streakRepository.findAll().stream().sorted((a, b) -> {
			int streakCmp = Integer.compare(b.getCurrentStreak(), a.getCurrentStreak());
			if (streakCmp != 0)
				return streakCmp;
			long completedA = enrollmentRepository.countByStudentIdAndStatus(a.getStudentId(), EnrollmentStatus.COMPLETED);
			long completedB = enrollmentRepository.countByStudentIdAndStatus(b.getStudentId(), EnrollmentStatus.COMPLETED);
			int completedCmp = Long.compare(completedB, completedA);
			if (completedCmp != 0)
				return completedCmp;
			long badgesA = badgeRepository.countByStudentId(a.getStudentId());
			long badgesB = badgeRepository.countByStudentId(b.getStudentId());
			return Long.compare(badgesB, badgesA);
		}).toList();
		List<LeaderboardResponseDto> board = new ArrayList<>();
		int rank = 1;
		for (LearningStreak s : streaks) {
			LeaderboardResponseDto dto = new LeaderboardResponseDto();
			dto.setRank(rank++);
			dto.setStudentId(s.getStudentId());
			dto.setCurrentStreak(s.getCurrentStreak());
			dto.setTotalBadges(badgeRepository.countByStudentId(s.getStudentId()));
			dto.setTotalCoursesCompleted(
					(int) enrollmentRepository.countByStudentIdAndStatus(s.getStudentId(), EnrollmentStatus.COMPLETED));
			userRepository.findById(s.getStudentId()).ifPresent(u -> dto.setStudentName(u.getFullName()));
			board.add(dto);
		}
		return ResponseEntity.ok(ApiResponse.success("Leaderboard", board));
	}
}