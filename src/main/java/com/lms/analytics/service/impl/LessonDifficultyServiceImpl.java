package com.lms.analytics.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.lms.analytics.dto.response.LessonDifficultyResponseDto;
import com.lms.analytics.service.LessonDifficultyService;
import com.lms.analytics.vo.DifficultyScoreVO;
import com.lms.course.entity.Lesson;
import com.lms.course.repository.LessonRepository;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.LearningEventType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonDifficultyServiceImpl implements LessonDifficultyService {

	private final LessonRepository lessonRepository;
	private final LearningEventRepository learningEventRepository;
	private final LessonProgressRepository lessonProgressRepository;

	@Override
	public List<LessonDifficultyResponseDto> getDifficultyForCourse(Long courseId) {
		return lessonRepository.findByCourseIdOrderByOrderIndex(courseId).stream().map(this::computeDifficulty)
				.toList();
	}

	private LessonDifficultyResponseDto computeDifficulty(Lesson lesson) {
		Long lessonId = lesson.getId();
		List<com.lms.enrollment.entity.LearningEvent> events = learningEventRepository.findByLessonId(lessonId);

		long totalAttempts = learningEventRepository.countByLessonIdAndEventType(lessonId,
				LearningEventType.QUIZ_ATTEMPT);
		long failedAttempts = totalAttempts > 0
				? totalAttempts - learningEventRepository.countByLessonIdAndEventTypeAndValueGreaterThanEqual(lessonId,
						LearningEventType.QUIZ_SCORE, BigDecimal.valueOf(50))
				: 0;
		double quizFailureRate = totalAttempts > 0 ? (double) failedAttempts / totalAttempts : 0.0;

		long totalViews = learningEventRepository.countByLessonIdAndEventType(lessonId, LearningEventType.VIDEO_WATCH);
		long rewinds = learningEventRepository.countByLessonIdAndEventType(lessonId, LearningEventType.VIDEO_REWIND);
		double rewindRate = totalViews > 0 ? (double) rewinds / totalViews : 0.0;

		Set<Long> startedLearners = events.stream().map(com.lms.enrollment.entity.LearningEvent::getStudentId)
				.filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
		long started = startedLearners.size();
		long completed = lessonProgressRepository.countByLessonIdAndIsCompletedTrue(lessonId);
		double dropOffRate = started > 0 ? Math.max(0, (started - completed) / (double) started) : 0.0;

		double score = (0.5 * quizFailureRate) + (0.3 * rewindRate) + (0.2 * dropOffRate);
		DifficultyScoreVO scoreVO = new DifficultyScoreVO(score);

		LessonDifficultyResponseDto dto = new LessonDifficultyResponseDto();
		dto.setLessonId(lessonId);
		dto.setLessonTitle(lesson.getTitle());
		dto.setDifficultyScore(score);
		dto.setDifficultyLevel(scoreVO.label());
		dto.setQuizFailureRate(quizFailureRate);
		dto.setRewindRate(rewindRate);
		dto.setDropOffRate(dropOffRate);
		return dto;
	}
}