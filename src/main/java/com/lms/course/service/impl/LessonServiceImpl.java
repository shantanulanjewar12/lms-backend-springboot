package com.lms.course.service.impl;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateLessonRequestDto;
import com.lms.course.dto.response.LessonResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.LessonService;
import com.lms.course.service.S3StorageService;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

	private final LessonRepository lessonRepository;
	private final CourseService courseService;
	private final S3StorageService s3StorageService;
	private final ModelMapper modelMapper;

	@Override
	public LessonResponseDto createLesson(Long instructorId, CreateLessonRequestDto request, MultipartFile file) {
		Course course = courseService.getCourseEntityById(request.getCourseId());
		if (!course.getInstructorId().equals(instructorId))
			throw new UnauthorizedException("You do not own this course");

		String contentUrl = null;
		if (file != null && !file.isEmpty()) {
			contentUrl = s3StorageService.uploadFile(file, "lessons/" + request.getCourseId());
		}

		Lesson lesson = Lesson.builder().course(course).title(request.getTitle()).contentType(request.getContentType())
				.contentUrl(contentUrl).orderIndex(request.getOrderIndex()).isFreePreview(request.getIsFreePreview())
				.durationSeconds(request.getDurationSeconds()).build();
		return toLessonResponseDto(lessonRepository.save(lesson));
	}

	@Override
	public List<LessonResponseDto> getLessonsByCourse(Long courseId, Long userId, String role) {
		return lessonRepository.findByCourseIdOrderByOrderIndex(courseId).stream().map(this::toLessonResponseDto)
				.toList();
	}

	@Override
	public LessonResponseDto updateLesson(Long lessonId, Long instructorId, CreateLessonRequestDto request,
			MultipartFile file) {
		Lesson lesson = getLessonEntityById(lessonId);
		if (!lesson.getCourse().getInstructorId().equals(instructorId))
			throw new UnauthorizedException("Not authorized");
		if (request.getTitle() != null)
			lesson.setTitle(request.getTitle());
		if (request.getContentType() != null)
			lesson.setContentType(request.getContentType());
		if (request.getOrderIndex() != null)
			lesson.setOrderIndex(request.getOrderIndex());
		if (request.getIsFreePreview() != null)
			lesson.setIsFreePreview(request.getIsFreePreview());
		if (file != null && !file.isEmpty()) {
			lesson.setContentUrl(s3StorageService.uploadFile(file, "lessons/" + lesson.getCourse().getId()));
		}
		return toLessonResponseDto(lessonRepository.save(lesson));
	}

	@Override
	public void deleteLesson(Long lessonId, Long instructorId) {
		Lesson lesson = getLessonEntityById(lessonId);
		if (!lesson.getCourse().getInstructorId().equals(instructorId))
			throw new UnauthorizedException("Not authorized");
		lessonRepository.delete(lesson);
	}

	@Override
	public Lesson getLessonEntityById(Long id) {
		return lessonRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson", id));
	}

	private LessonResponseDto toLessonResponseDto(Lesson lesson) {
		LessonResponseDto dto = modelMapper.map(lesson, LessonResponseDto.class);
		dto.setContentUrl(s3StorageService.getAccessibleFileUrl(dto.getContentUrl()));
		dto.setCourseId(lesson.getCourse().getId());
		return dto;
	}
}