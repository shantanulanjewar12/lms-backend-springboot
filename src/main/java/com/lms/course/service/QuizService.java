package com.lms.course.service;

import java.util.List;

import com.lms.course.dto.request.QuizQuestionUpsertRequestDto;
import com.lms.course.dto.request.QuizSubmitRequestDto;
import com.lms.course.dto.response.QuizQuestionResponseDto;
import com.lms.course.dto.response.QuizSubmitResponseDto;

public interface QuizService {

	List<QuizQuestionResponseDto> upsertQuizQuestions(Long instructorId, Long lessonId,
			List<QuizQuestionUpsertRequestDto> request);

	List<QuizQuestionResponseDto> getQuizQuestions(Long lessonId, Long userId, String role);

	QuizSubmitResponseDto submitQuiz(Long studentId, Long lessonId, QuizSubmitRequestDto request);
}
