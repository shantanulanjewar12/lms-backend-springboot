package com.lms.course.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateCourseRequestDto;
import com.lms.course.dto.request.UpdateCourseRequestDto;
import com.lms.course.dto.response.CourseResponseDto;
import com.lms.course.dto.response.CourseSummaryResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.ReviewRepository;
import com.lms.course.service.S3StorageService;
import com.lms.course.vo.CourseLevel;
import com.lms.course.vo.CourseStatus;
import com.lms.notification.service.EmailService;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course course;
    private CourseResponseDto courseResponseDto;
    private CourseSummaryResponseDto courseSummaryResponseDto;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .instructorId(100L)
                .title("Java Masterclass")
                .description("Complete Java course")
                .price(BigDecimal.valueOf(999))
                .currency("INR")
                .level(CourseLevel.BEGINNER)
                .category("Programming")
                .status(CourseStatus.DRAFT)
                .thumbnailUrl("thumb.jpg")
                .build();

        courseResponseDto = new CourseResponseDto();
        courseResponseDto.setId(1L);
        courseResponseDto.setInstructorId(100L);
        courseResponseDto.setTitle("Java Masterclass");
        courseResponseDto.setDescription("Complete Java course");
        courseResponseDto.setPrice(BigDecimal.valueOf(999));
        courseResponseDto.setCurrency("INR");
        courseResponseDto.setLevel(CourseLevel.BEGINNER);
        courseResponseDto.setCategory("Programming");
        courseResponseDto.setStatus(CourseStatus.DRAFT);
        courseResponseDto.setThumbnailUrl("thumb.jpg");

        courseSummaryResponseDto = new CourseSummaryResponseDto();
        courseSummaryResponseDto.setId(1L);
        courseSummaryResponseDto.setTitle("Java Masterclass");
        courseSummaryResponseDto.setPrice(BigDecimal.valueOf(999));
        courseSummaryResponseDto.setCategory("Programming");
        courseSummaryResponseDto.setLevel(CourseLevel.BEGINNER);
        courseSummaryResponseDto.setThumbnailUrl("thumb.jpg");
    }

    @Test
    void createCourse_shouldCreateCourseAndSendEmail() {
        CreateCourseRequestDto request = new CreateCourseRequestDto();
        request.setTitle("Java Masterclass");
        request.setDescription("Complete Java course");
        request.setPrice(BigDecimal.valueOf(999));
        request.setCurrency("INR");
        request.setLevel(CourseLevel.BEGINNER);
        request.setCategory("Programming");

        UserResponseDto instructor = new UserResponseDto();
        instructor.setId(100L);
        instructor.setEmail("instructor@gmail.com");
        instructor.setFullName("Priya");

        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(userService.getUserById(100L)).thenReturn(instructor);
        doNothing().when(emailService).sendCourseCreatedEmail(
                "instructor@gmail.com", "Priya", "Java Masterclass");

        when(modelMapper.map(course, CourseResponseDto.class)).thenReturn(courseResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.5);
        when(reviewRepository.countByCourseId(1L)).thenReturn(10L);
        when(lessonRepository.countByCourseId(1L)).thenReturn(5L);

        CourseResponseDto result = courseService.createCourse(100L, request);

        assertNotNull(result);
        assertEquals("Java Masterclass", result.getTitle());
        assertEquals("accessible-thumb.jpg", result.getThumbnailUrl());
        assertEquals(4.5, result.getAverageRating());
        assertEquals(10L, result.getTotalReviews());
        assertEquals(5L, result.getTotalLessons());

        verify(courseRepository).save(any(Course.class));
        verify(userService).getUserById(100L);
        verify(emailService).sendCourseCreatedEmail("instructor@gmail.com", "Priya", "Java Masterclass");
    }

    @Test
    void createCourse_shouldUseDefaultPriceAndCurrency_whenNull() {
        CreateCourseRequestDto request = new CreateCourseRequestDto();
        request.setTitle("Spring Boot");
        request.setDescription("Backend course");
        request.setPrice(null);
        request.setCurrency(null);
        request.setLevel(CourseLevel.INTERMEDIATE);
        request.setCategory("Backend");

        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course savedCourse = invocation.getArgument(0);
            savedCourse.setId(2L);
            return savedCourse;
        });

        Course mappedCourse = Course.builder()
                .id(2L)
                .instructorId(101L)
                .title("Spring Boot")
                .description("Backend course")
                .price(BigDecimal.ZERO)
                .currency("INR")
                .level(CourseLevel.INTERMEDIATE)
                .category("Backend")
                .status(CourseStatus.DRAFT)
                .build();

        CourseResponseDto mappedDto = new CourseResponseDto();
        mappedDto.setId(2L);
        mappedDto.setTitle("Spring Boot");
        mappedDto.setPrice(BigDecimal.ZERO);
        mappedDto.setCurrency("INR");

        when(modelMapper.map(any(Course.class), eq(CourseResponseDto.class))).thenReturn(mappedDto);
        when(s3StorageService.getAccessibleFileUrl(null)).thenReturn(null);
        when(reviewRepository.findAverageRatingByCourseId(2L)).thenReturn(0.0);
        when(reviewRepository.countByCourseId(2L)).thenReturn(0L);
        when(lessonRepository.countByCourseId(2L)).thenReturn(0L);
        when(userService.getUserById(101L)).thenThrow(new RuntimeException("Email lookup failed"));

        CourseResponseDto result = courseService.createCourse(101L, request);

        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getPrice());
        assertEquals("INR", result.getCurrency());
    }

    @Test
    void getInstructorCourses_shouldReturnPageOfCourseSummary() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(course));

        when(courseRepository.findByInstructorId(100L, pageable)).thenReturn(page);
        when(modelMapper.map(course, CourseSummaryResponseDto.class)).thenReturn(courseSummaryResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.2);
        when(lessonRepository.countByCourseId(1L)).thenReturn(6L);

        Page<CourseSummaryResponseDto> result = courseService.getInstructorCourses(100L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Java Masterclass", result.getContent().get(0).getTitle());
        assertEquals("accessible-thumb.jpg", result.getContent().get(0).getThumbnailUrl());
        assertEquals(4.2, result.getContent().get(0).getAverageRating());
        assertEquals(6L, result.getContent().get(0).getTotalLessons());
    }

    @Test
    void getPublishedCourses_shouldReturnPublishedCourses_whenNoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        course.setStatus(CourseStatus.PUBLISHED);
        Page<Course> page = new PageImpl<>(List.of(course));

        when(courseRepository.findByStatus(CourseStatus.PUBLISHED, pageable)).thenReturn(page);
        when(modelMapper.map(course, CourseSummaryResponseDto.class)).thenReturn(courseSummaryResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.8);
        when(lessonRepository.countByCourseId(1L)).thenReturn(8L);

        Page<CourseSummaryResponseDto> result = courseService.getPublishedCourses(null, null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(courseRepository).findByStatus(CourseStatus.PUBLISHED, pageable);
    }

    @Test
    void getPublishedCourses_shouldReturnPublishedCourses_whenCategoryProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        course.setStatus(CourseStatus.PUBLISHED);
        Page<Course> page = new PageImpl<>(List.of(course));

        when(courseRepository.findByStatusAndCategory(CourseStatus.PUBLISHED, "Programming", pageable))
                .thenReturn(page);
        when(modelMapper.map(course, CourseSummaryResponseDto.class)).thenReturn(courseSummaryResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.0);
        when(lessonRepository.countByCourseId(1L)).thenReturn(7L);

        Page<CourseSummaryResponseDto> result = courseService.getPublishedCourses("Programming", null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(courseRepository).findByStatusAndCategory(CourseStatus.PUBLISHED, "Programming", pageable);
    }

    @Test
    void getPublishedCourses_shouldReturnPublishedCourses_whenLevelProvided() {
        Pageable pageable = PageRequest.of(0, 10);
        course.setStatus(CourseStatus.PUBLISHED);
        Page<Course> page = new PageImpl<>(List.of(course));

        when(courseRepository.findByStatusAndLevel(CourseStatus.PUBLISHED, CourseLevel.BEGINNER, pageable))
                .thenReturn(page);
        when(modelMapper.map(course, CourseSummaryResponseDto.class)).thenReturn(courseSummaryResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.0);
        when(lessonRepository.countByCourseId(1L)).thenReturn(7L);

        Page<CourseSummaryResponseDto> result = courseService.getPublishedCourses(null, "beginner", pageable);

        assertEquals(1, result.getTotalElements());
        verify(courseRepository).findByStatusAndLevel(CourseStatus.PUBLISHED, CourseLevel.BEGINNER, pageable);
    }

    @Test
    void getCourseById_shouldReturnCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(modelMapper.map(course, CourseResponseDto.class)).thenReturn(courseResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.6);
        when(reviewRepository.countByCourseId(1L)).thenReturn(12L);
        when(lessonRepository.countByCourseId(1L)).thenReturn(9L);

        CourseResponseDto result = courseService.getCourseById(1L);

        assertNotNull(result);
        assertEquals("Java Masterclass", result.getTitle());
        assertEquals(4.6, result.getAverageRating());
        assertEquals(12L, result.getTotalReviews());
        assertEquals(9L, result.getTotalLessons());
    }

    @Test
    void getCourseById_shouldThrowException_whenCourseNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getCourseById(1L));
    }

    @Test
    void updateCourse_shouldUpdateCourse_whenInstructorOwnsCourse() {
        UpdateCourseRequestDto request = new UpdateCourseRequestDto();
        request.setTitle("Updated Title");
        request.setDescription("Updated Description");
        request.setPrice(BigDecimal.valueOf(1200));
        request.setLevel(CourseLevel.ADVANCED);
        request.setCategory("Advanced Programming");
        request.setStatus(CourseStatus.PUBLISHED);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(Course.class), eq(CourseResponseDto.class))).thenReturn(courseResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.5);
        when(reviewRepository.countByCourseId(1L)).thenReturn(10L);
        when(lessonRepository.countByCourseId(1L)).thenReturn(5L);

        CourseResponseDto result = courseService.updateCourse(1L, 100L, request);

        assertNotNull(result);
        verify(courseRepository).save(any(Course.class));
    }

    @Test
    void updateCourse_shouldThrowException_whenInstructorDoesNotOwnCourse() {
        UpdateCourseRequestDto request = new UpdateCourseRequestDto();
        request.setTitle("Updated Title");

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(UnauthorizedException.class,
                () -> courseService.updateCourse(1L, 999L, request));
    }

    @Test
    void deleteCourse_shouldDeleteCourse_whenInstructorOwnsCourse() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(1L, 100L, "INSTRUCTOR");

        verify(courseRepository).delete(course);
    }

    @Test
    void deleteCourse_shouldDeleteCourse_whenRoleIsAdmin() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(1L, 999L, "ADMIN");

        verify(courseRepository).delete(course);
    }

    @Test
    void deleteCourse_shouldThrowException_whenUnauthorized() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(UnauthorizedException.class,
                () -> courseService.deleteCourse(1L, 999L, "INSTRUCTOR"));

        verify(courseRepository, never()).delete(any(Course.class));
    }

    @Test
    void publishCourse_shouldPublishCourse_whenValidDraftAndOwner() {
        course.setStatus(CourseStatus.DRAFT);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelMapper.map(any(Course.class), eq(CourseResponseDto.class))).thenReturn(courseResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.4);
        when(reviewRepository.countByCourseId(1L)).thenReturn(15L);
        when(lessonRepository.countByCourseId(1L)).thenReturn(11L);

        CourseResponseDto result = courseService.publishCourse(1L, 100L);

        assertNotNull(result);
        assertEquals(CourseStatus.PUBLISHED, course.getStatus());
        verify(courseRepository).save(course);
    }

    @Test
    void publishCourse_shouldThrowException_whenNotOwner() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(UnauthorizedException.class,
                () -> courseService.publishCourse(1L, 999L));
    }

    @Test
    void publishCourse_shouldThrowException_whenCourseNotDraft() {
        course.setStatus(CourseStatus.PUBLISHED);
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(BadRequestException.class,
                () -> courseService.publishCourse(1L, 100L));
    }

    @Test
    void uploadThumbnail_shouldUploadAndSaveThumbnail_whenOwner() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "thumbnail.jpg",
                "image/jpeg",
                "dummy image".getBytes()
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("thumbnails")))
                .thenReturn("https://s3.aws.com/thumbnail.jpg");

        String result = courseService.uploadThumbnail(1L, 100L, file);

        assertEquals("https://s3.aws.com/thumbnail.jpg", result);
        assertEquals("https://s3.aws.com/thumbnail.jpg", course.getThumbnailUrl());
        verify(courseRepository).save(course);
    }

    @Test
    void uploadThumbnail_shouldThrowException_whenNotOwner() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "thumbnail.jpg",
                "image/jpeg",
                "dummy image".getBytes()
        );

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        assertThrows(UnauthorizedException.class,
                () -> courseService.uploadThumbnail(1L, 999L, file));

        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void getCourseEntityById_shouldReturnCourseEntity() {
        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

        Course result = courseService.getCourseEntityById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java Masterclass", result.getTitle());
    }

    @Test
    void getCourseEntityById_shouldThrowException_whenNotFound() {
        when(courseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCourseEntityById(1L));
    }

    @Test
    void createCourse_shouldStillSucceed_whenEmailSendingFails() {
        CreateCourseRequestDto request = new CreateCourseRequestDto();
        request.setTitle("Java Masterclass");
        request.setDescription("Complete Java course");
        request.setPrice(BigDecimal.valueOf(999));
        request.setCurrency("INR");
        request.setLevel(CourseLevel.BEGINNER);
        request.setCategory("Programming");

        UserResponseDto instructor = new UserResponseDto();
        instructor.setId(100L);
        instructor.setEmail("instructor@gmail.com");
        instructor.setFullName("Priya");

        when(courseRepository.save(any(Course.class))).thenReturn(course);
        when(userService.getUserById(100L)).thenReturn(instructor);

        org.mockito.Mockito.doThrow(new RuntimeException("Mail failed"))
                .when(emailService)
                .sendCourseCreatedEmail(any(), any(), any());

        when(modelMapper.map(course, CourseResponseDto.class)).thenReturn(courseResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(reviewRepository.findAverageRatingByCourseId(1L)).thenReturn(4.5);
        when(reviewRepository.countByCourseId(1L)).thenReturn(10L);
        when(lessonRepository.countByCourseId(1L)).thenReturn(5L);

        CourseResponseDto result = courseService.createCourse(100L, request);

        assertNotNull(result);
        assertEquals("Java Masterclass", result.getTitle());
        verify(courseRepository).save(any(Course.class));
        verify(emailService).sendCourseCreatedEmail(any(), any(), any());
    }
}
