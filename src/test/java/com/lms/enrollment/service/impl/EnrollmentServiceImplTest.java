package com.lms.enrollment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lms.course.entity.Course;
import com.lms.course.repository.LessonRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.S3StorageService;
import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.entity.Enrollment;
import com.lms.enrollment.repository.EnrollmentRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.enrollment.vo.EnrollmentStatus;
import com.lms.notification.service.EmailService;
import com.lms.shared.exception.BadRequestException;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private Course course;
    private Enrollment enrollment;
    private EnrollmentResponseDto enrollmentResponseDto;
    private UserResponseDto student;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(101L)
                .title("Java Masterclass")
                .thumbnailUrl("thumb.jpg")
                .build();

        enrollment = Enrollment.builder()
                .id(1L)
                .studentId(10L)
                .courseId(101L)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        enrollmentResponseDto = new EnrollmentResponseDto();
        enrollmentResponseDto.setId(1L);
        enrollmentResponseDto.setStudentId(10L);
        enrollmentResponseDto.setCourseId(101L);
        enrollmentResponseDto.setStatus(EnrollmentStatus.ACTIVE);

        student = new UserResponseDto();
        student.setId(10L);
        student.setEmail("student@gmail.com");
        student.setFullName("Priya");
    }

    @Test
    void enroll_shouldCreateEnrollmentSuccessfully() {
        EnrollRequestDto request = new EnrollRequestDto();
        request.setCourseId(101L);

        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 101L)).thenReturn(false);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
        when(userService.getUserById(10L)).thenReturn(student);
        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(2L);

        EnrollmentResponseDto result = enrollmentService.enroll(10L, request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java Masterclass", result.getCourseTitle());
        assertEquals("accessible-thumb.jpg", result.getCourseThumbnailUrl());
        assertEquals(20.0, result.getCompletionPercentage());
        verify(emailService).sendEnrollmentEmail("student@gmail.com", "Priya", "Java Masterclass");
    }

    @Test
    void createEnrollmentDirectly_shouldThrowException_whenAlreadyEnrolled() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 101L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> enrollmentService.createEnrollmentDirectly(10L, 101L));

        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void createEnrollmentDirectly_shouldStillSucceed_whenEmailFails() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 101L)).thenReturn(false);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(enrollment);
        when(userService.getUserById(10L)).thenReturn(student);
        org.mockito.Mockito.doThrow(new RuntimeException("mail failed"))
                .when(emailService).sendEnrollmentEmail(any(), any(), any());

        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(3L);

        EnrollmentResponseDto result = enrollmentService.createEnrollmentDirectly(10L, 101L);

        assertNotNull(result);
        assertEquals("Java Masterclass", result.getCourseTitle());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void getMyEnrollments_shouldReturnEnrollmentList() {
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(List.of(enrollment));
        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(5L);

        List<EnrollmentResponseDto> result = enrollmentService.getMyEnrollments(10L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Java Masterclass", result.get(0).getCourseTitle());
        assertEquals(50.0, result.get(0).getCompletionPercentage());
    }

    @Test
    void isEnrolled_shouldReturnTrue_whenExists() {
        when(enrollmentRepository.existsByStudentIdAndCourseId(10L, 101L)).thenReturn(true);

        boolean result = enrollmentService.isEnrolled(10L, 101L);

        assertEquals(true, result);
    }

    @Test
    void getStudentsByCourse_shouldReturnEnrollments() {
        when(enrollmentRepository.findByCourseId(101L)).thenReturn(List.of(enrollment));
        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(8L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(4L);

        List<EnrollmentResponseDto> result = enrollmentService.getStudentsByCourse(101L);

        assertEquals(1, result.size());
        assertEquals(50.0, result.get(0).getCompletionPercentage());
    }

    @Test
    void getEnrollmentEntity_shouldReturnEnrollment() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 101L))
                .thenReturn(Optional.of(enrollment));

        Enrollment result = enrollmentService.getEnrollmentEntity(10L, 101L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getEnrollmentEntity_shouldThrowException_whenNotFound() {
        when(enrollmentRepository.findByStudentIdAndCourseId(10L, 101L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> enrollmentService.getEnrollmentEntity(10L, 101L));
    }

    @Test
    void toDto_shouldMarkCompleted_whenAllLessonsDone() {
        when(enrollmentRepository.findByStudentId(10L)).thenReturn(List.of(enrollment));
        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(5L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(5L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<EnrollmentResponseDto> result = enrollmentService.getMyEnrollments(10L);

        assertEquals(1, result.size());
        assertEquals(EnrollmentStatus.COMPLETED, result.get(0).getStatus());
        assertEquals(100.0, result.get(0).getCompletionPercentage());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void toDto_shouldRevertToActive_whenCompletedStatusButLessonsNotDone() {
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setCompletedAt(LocalDateTime.now());

        enrollmentResponseDto.setStatus(EnrollmentStatus.COMPLETED);
        enrollmentResponseDto.setCompletedAt(enrollment.getCompletedAt());

        when(enrollmentRepository.findByStudentId(10L)).thenReturn(List.of(enrollment));
        when(modelMapper.map(enrollment, EnrollmentResponseDto.class)).thenReturn(enrollmentResponseDto);
        when(courseService.getCourseEntityById(101L)).thenReturn(course);
        when(s3StorageService.getAccessibleFileUrl("thumb.jpg")).thenReturn("accessible-thumb.jpg");
        when(lessonRepository.countByCourseId(101L)).thenReturn(10L);
        when(lessonProgressRepository.countByEnrollmentIdAndIsCompletedTrue(1L)).thenReturn(7L);
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<EnrollmentResponseDto> result = enrollmentService.getMyEnrollments(10L);

        assertEquals(1, result.size());
        assertEquals(EnrollmentStatus.ACTIVE, result.get(0).getStatus());
        assertEquals(70.0, result.get(0).getCompletionPercentage());
        verify(enrollmentRepository).save(any(Enrollment.class));
    }
}