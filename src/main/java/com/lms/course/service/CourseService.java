package com.lms.course.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateCourseRequestDto;
import com.lms.course.dto.request.UpdateCourseRequestDto;
import com.lms.course.dto.response.CourseResponseDto;
import com.lms.course.dto.response.CourseSummaryResponseDto;
import com.lms.course.entity.Course;

public interface CourseService {
	CourseResponseDto createCourse(Long instructorId, CreateCourseRequestDto request);

	Page<CourseSummaryResponseDto> getInstructorCourses(Long instructorId, Pageable pageable);

	Page<CourseSummaryResponseDto> getPublishedCourses(String category, String level, Pageable pageable);

	CourseResponseDto getCourseById(Long id);

	CourseResponseDto updateCourse(Long courseId, Long instructorId, UpdateCourseRequestDto request);

	void deleteCourse(Long courseId, Long userId, String role);

	CourseResponseDto publishCourse(Long courseId, Long instructorId);

	String uploadThumbnail(Long courseId, Long instructorId, MultipartFile file);

	Course getCourseEntityById(Long id);
}