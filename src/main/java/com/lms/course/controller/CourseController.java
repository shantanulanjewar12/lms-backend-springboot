package com.lms.course.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateCourseRequestDto;
import com.lms.course.dto.request.UpdateCourseRequestDto;
import com.lms.course.dto.response.CourseResponseDto;
import com.lms.course.dto.response.CourseSummaryResponseDto;
import com.lms.course.service.CourseService;
import com.lms.shared.dto.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

	
	private final CourseService courseService;

	
	@PostMapping
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<CourseResponseDto>> createCourse(Authentication auth,
			@Valid @RequestBody CreateCourseRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Course created", courseService.createCourse(userId, request)));
	}

	
	
	@GetMapping
	public ResponseEntity<ApiResponse<Page<CourseSummaryResponseDto>>> getPublishedCourses(
			@RequestParam(required = false) String category, @RequestParam(required = false) String level,
			@RequestParam(required = false) String search,
			@PageableDefault(size = 10) Pageable pageable) {
		return ResponseEntity.ok(
				ApiResponse.success("Courses fetched", courseService.getPublishedCourses(category, level, search, pageable)));
	}

	
	
	@GetMapping("/instructor/my")
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<Page<CourseSummaryResponseDto>>> getMyCourses(Authentication auth,
			@PageableDefault(size = 10) Pageable pageable) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Instructor courses fetched",
				courseService.getInstructorCourses(userId, pageable)));
	}

	
	
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<CourseResponseDto>> getCourse(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Course fetched", courseService.getCourseById(id)));
	}

	
	
	@PutMapping("/{id}")
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<CourseResponseDto>> updateCourse(@PathVariable Long id, Authentication auth,
			@RequestBody UpdateCourseRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Course updated", courseService.updateCourse(id, userId, request)));
	}
	
	

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('INSTRUCTOR','ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id, Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		String role = auth.getAuthorities().iterator().next().getAuthority();
		courseService.deleteCourse(id, userId, role);
		return ResponseEntity.ok(ApiResponse.success("Course deleted"));
	}
	
	

	@PatchMapping("/{id}/publish")
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<CourseResponseDto>> publishCourse(@PathVariable Long id, Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Course published", courseService.publishCourse(id, userId)));
	}
	
	
	

	@PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('INSTRUCTOR')")
	public ResponseEntity<ApiResponse<String>> uploadThumbnail(@PathVariable Long id, Authentication auth,
			@RequestParam("file") MultipartFile file) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Thumbnail uploaded", courseService.uploadThumbnail(id, userId, file)));
	}
}