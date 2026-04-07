package com.lms.course.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.lms.course.dto.request.QuizAnswerItemRequestDto;
import com.lms.course.dto.request.QuizQuestionUpsertRequestDto;
import com.lms.course.dto.request.QuizSubmitRequestDto;
import com.lms.course.dto.response.QuizAttemptResponseDto;
import com.lms.course.dto.response.QuizQuestionResponseDto;
import com.lms.course.dto.response.QuizSubmitResponseDto;
import com.lms.course.entity.Lesson;
import com.lms.course.entity.QuizAttempt;
import com.lms.course.entity.QuizQuestion;
import com.lms.course.repository.QuizAttemptRepository;
import com.lms.course.repository.QuizQuestionRepository;
import com.lms.course.service.LessonService;
import com.lms.course.service.QuizService;
import com.lms.course.vo.ContentType;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.vo.LearningEventType;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

	private static final double PASS_PERCENTAGE = 50.0;

	private final QuizQuestionRepository quizQuestionRepository;
	private final LessonService lessonService;
	private final EnrollmentRepository enrollmentRepository;
	private final LearningEventRepository learningEventRepository;
	private final QuizAttemptRepository quizAttemptRepository;
	private final ObjectMapper objectMapper;

	@Override
	public List<QuizQuestionResponseDto> upsertQuizQuestions(Long instructorId, Long lessonId,
			List<QuizQuestionUpsertRequestDto> request) {
		if (request == null || request.isEmpty()) {
			throw new BadRequestException("Quiz must have at least one question");
		}

		Lesson lesson = lessonService.getLessonEntityById(lessonId);
		validateQuizLesson(lesson);
		if (!lesson.getCourse().getInstructorId().equals(instructorId)) {
			throw new UnauthorizedException("You do not own this lesson");
		}

		quizQuestionRepository.deleteByLessonId(lessonId);

		List<QuizQuestion> entities = request.stream().map(q -> QuizQuestion.builder().lesson(lesson)
				.questionText(q.getQuestionText().trim()).optionA(q.getOptionA().trim()).optionB(q.getOptionB().trim())
				.optionC(q.getOptionC().trim()).optionD(q.getOptionD().trim())
				.correctOptions(String.join(",", normalizeOptions(q.getCorrectOptions()))).orderIndex(q.getOrderIndex()).build()).toList();
		quizQuestionRepository.saveAll(entities);

		return quizQuestionRepository.findByLessonIdOrderByOrderIndex(lessonId).stream().map(this::toQuestionDto).toList();
	}

	@Override
	public List<QuizQuestionResponseDto> getQuizQuestions(Long lessonId, Long userId, String role) {
		Lesson lesson = lessonService.getLessonEntityById(lessonId);
		validateQuizLesson(lesson);
		boolean includeAnswers = role != null && (role.contains("INSTRUCTOR") || role.contains("ADMIN"));

		if (role != null && role.contains("INSTRUCTOR") && !lesson.getCourse().getInstructorId().equals(userId)) {
			throw new UnauthorizedException("You do not own this lesson");
		}

		if (role != null && role.contains("STUDENT")) {
			validateStudentCanAccessQuiz(userId, lesson);
		}

		return quizQuestionRepository.findByLessonIdOrderByOrderIndex(lessonId).stream().map(q -> {
			QuizQuestionResponseDto dto = toQuestionDto(q);
			if (!includeAnswers) {
				dto.setCorrectOptions(null);
			}
			return dto;
		}).toList();
	}

	@Override
	public QuizSubmitResponseDto submitQuiz(Long studentId, Long lessonId, QuizSubmitRequestDto request) {
		Lesson lesson = lessonService.getLessonEntityById(lessonId);
		validateQuizLesson(lesson);
		validateStudentCanAccessQuiz(studentId, lesson);

		List<QuizQuestion> questions = quizQuestionRepository.findByLessonIdOrderByOrderIndex(lessonId);
		if (questions.isEmpty()) {
			throw new BadRequestException("Quiz has no questions yet");
		}

		Map<Long, QuizAnswerItemRequestDto> submittedAnswers = request.getAnswers().stream().collect(
				Collectors.toMap(QuizAnswerItemRequestDto::getQuestionId, Function.identity(), (first, second) -> first));

		Map<Long, List<String>> answersToPersist = submittedAnswers.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				e -> normalizeOptions(e.getValue().getSelectedOptions()),
				(first, second) -> first));

		int correct = 0;
		int attempted = 0;
		List<QuizSubmitResponseDto.AnswerResultItemDto> resultItems = questions.stream()
				.sorted(Comparator.comparing(QuizQuestion::getOrderIndex)).map(q -> {
					QuizAnswerItemRequestDto answer = submittedAnswers.get(q.getId());
					List<String> selectedOptions = answer != null ? normalizeOptions(answer.getSelectedOptions())
							: List.of();
					List<String> expectedOptions = parseOptions(q.getCorrectOptions());
					boolean isCorrect = !selectedOptions.isEmpty() &&
							new LinkedHashSet<>(selectedOptions).equals(new LinkedHashSet<>(expectedOptions));

					QuizSubmitResponseDto.AnswerResultItemDto item = new QuizSubmitResponseDto.AnswerResultItemDto();
					item.setQuestionId(q.getId());
					item.setSelectedOptions(selectedOptions);
					item.setCorrectOptions(expectedOptions);
					item.setIsCorrect(isCorrect);
					return item;
				}).toList();

		for (QuizSubmitResponseDto.AnswerResultItemDto item : resultItems) {
			if (item.getSelectedOptions() != null && !item.getSelectedOptions().isEmpty()) {
				attempted++;
			}
			if (Boolean.TRUE.equals(item.getIsCorrect())) {
				correct++;
			}
		}

		double scorePercent = questions.isEmpty() ? 0.0 : (correct * 100.0) / questions.size();
		boolean passed = scorePercent >= PASS_PERCENTAGE;

		learningEventRepository.save(LearningEvent.builder().studentId(studentId).lessonId(lessonId)
				.eventType(LearningEventType.QUIZ_ATTEMPT).value(BigDecimal.ONE).build());
		learningEventRepository.save(LearningEvent.builder().studentId(studentId).lessonId(lessonId)
				.eventType(LearningEventType.QUIZ_SCORE).value(BigDecimal.valueOf(scorePercent)).build());

		try {
			QuizAttempt attempt = QuizAttempt.builder().studentId(studentId).courseId(lesson.getCourse().getId())
					.quizId(lessonId).answersJson(objectMapper.writeValueAsString(answersToPersist))
					.scorePercent(scorePercent).build();
			quizAttemptRepository.save(attempt);
		} catch (Exception e) {
			throw new BadRequestException("Failed to store quiz attempt");
		}

		QuizSubmitResponseDto response = new QuizSubmitResponseDto();
		response.setLessonId(lessonId);
		response.setTotalQuestions(questions.size());
		response.setAttemptedQuestions(attempted);
		response.setCorrectAnswers(correct);
		response.setScorePercent(scorePercent);
		response.setPassed(passed);
		response.setAnswerResults(resultItems);
		return response;
	}

	@Override
	public List<QuizAttemptResponseDto> getQuizAttempts(Long studentId, Long lessonId) {
		Lesson lesson = lessonService.getLessonEntityById(lessonId);
		validateQuizLesson(lesson);
		validateStudentCanAccessQuiz(studentId, lesson);

		return quizAttemptRepository.findByStudentIdAndQuizIdOrderBySubmittedAtDesc(studentId, lessonId).stream()
				.map(this::toAttemptDto).toList();
	}

	@Override
	public QuizAttemptResponseDto getLatestQuizAttempt(Long studentId, Long lessonId) {
		Lesson lesson = lessonService.getLessonEntityById(lessonId);
		validateQuizLesson(lesson);
		validateStudentCanAccessQuiz(studentId, lesson);

		QuizAttempt attempt = quizAttemptRepository.findTopByStudentIdAndQuizIdOrderBySubmittedAtDesc(studentId, lessonId)
				.orElseThrow(() -> new ResourceNotFoundException("Quiz attempt not found"));
		return toAttemptDto(attempt);
	}

	private void validateStudentCanAccessQuiz(Long studentId, Lesson lesson) {
		if (lesson.getIsFreePreview()) {
			return;
		}
		Enrollment enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, lesson.getCourse().getId())
				.orElseThrow(() -> new UnauthorizedException("Enroll to access this quiz"));
		if (enrollment.getExpiresAt() != null && LocalDateTime.now().isAfter(enrollment.getExpiresAt())) {
			throw new UnauthorizedException("Your course access period has expired.");
		}
	}

	private void validateQuizLesson(Lesson lesson) {
		if (lesson.getContentType() != ContentType.QUIZ) {
			throw new BadRequestException("Quiz questions are only allowed for QUIZ lessons");
		}
	}

	private QuizQuestionResponseDto toQuestionDto(QuizQuestion question) {
		QuizQuestionResponseDto dto = new QuizQuestionResponseDto();
		dto.setId(question.getId());
		dto.setLessonId(question.getLesson().getId());
		dto.setQuestionText(question.getQuestionText());
		dto.setOptionA(question.getOptionA());
		dto.setOptionB(question.getOptionB());
		dto.setOptionC(question.getOptionC());
		dto.setOptionD(question.getOptionD());
		dto.setCorrectOptions(parseOptions(question.getCorrectOptions()));
		dto.setOrderIndex(question.getOrderIndex());
		return dto;
	}

	private List<String> parseOptions(String csv) {
		if (csv == null || csv.isBlank()) {
			return List.of();
		}
		return normalizeOptions(List.of(csv.split(",")));
	}

	private List<String> normalizeOptions(List<String> options) {
		if (options == null) {
			throw new BadRequestException("Options cannot be null");
		}
		Set<String> normalized = options.stream().filter(o -> o != null && !o.isBlank()).map(o -> o.trim().toUpperCase())
				.filter(o -> o.equals("A") || o.equals("B") || o.equals("C") || o.equals("D"))
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (normalized.isEmpty()) {
			throw new BadRequestException("At least one valid option (A/B/C/D) is required");
		}
		return List.copyOf(normalized);
	}

	private QuizAttemptResponseDto toAttemptDto(QuizAttempt attempt) {
		QuizAttemptResponseDto dto = new QuizAttemptResponseDto();
		dto.setId(attempt.getId());
		dto.setStudentId(attempt.getStudentId());
		dto.setCourseId(attempt.getCourseId());
		dto.setQuizId(attempt.getQuizId());
		dto.setScorePercent(attempt.getScorePercent());
		dto.setSubmittedAt(attempt.getSubmittedAt());
		try {
			dto.setAnswers(objectMapper.readValue(attempt.getAnswersJson(), new TypeReference<Map<Long, List<String>>>() {
			}));
		} catch (Exception e) {
			dto.setAnswers(Map.of());
		}
		return dto;
	}
}
