package com.lms.course.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.dto.request.CreateLessonRequestDto;
import com.lms.course.dto.response.LessonResponseDto;
import com.lms.course.entity.Course;
import com.lms.course.entity.Lesson;
import com.lms.course.repository.LessonRepository;
import com.lms.course.repository.QuizQuestionRepository;
import com.lms.course.service.CourseService;
import com.lms.course.service.S3StorageService;
import com.lms.course.vo.ContentType;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.repository.LessonProgressRepository;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.shared.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private CourseService courseService;

    @Mock
    private S3StorageService s3StorageService;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private QuizQuestionRepository quizQuestionRepository;

    @Mock
    private LessonProgressRepository lessonProgressRepository;

    @Mock
    private LearningEventRepository learningEventRepository;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private Course course;
    private Lesson lesson;
    private LessonResponseDto lessonResponseDto;
    private CreateLessonRequestDto request;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(1L)
                .instructorId(100L)
                .title("Java Course")
                .build();

        lesson = Lesson.builder()
                .id(10L)
                .course(course)
                .title("Introduction")
                .contentType(ContentType.VIDEO)
                .contentUrl("lessons/1/intro.mp4")
                .orderIndex(1)
                .isFreePreview(true)
                .durationSeconds(300)
                .build();

        lessonResponseDto = new LessonResponseDto();
        lessonResponseDto.setId(10L);
        lessonResponseDto.setTitle("Introduction");
        lessonResponseDto.setContentType(ContentType.VIDEO);
        lessonResponseDto.setContentUrl("lessons/1/intro.mp4");
        lessonResponseDto.setOrderIndex(1);
        lessonResponseDto.setIsFreePreview(true);
        lessonResponseDto.setDurationSeconds(300);

        request = new CreateLessonRequestDto();
        request.setCourseId(1L);
        request.setTitle("Introduction");
        request.setContentType(ContentType.VIDEO);
        request.setOrderIndex(1);
        request.setIsFreePreview(true);
        request.setDurationSeconds(300);
    }

    @Test
    void createLesson_shouldCreateLessonWithFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "intro.mp4",
                "video/mp4",
                "dummy video content".getBytes()
        );

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("lessons/1")))
                .thenReturn("uploaded-file-url");
        when(lessonRepository.save(any(Lesson.class))).thenReturn(lesson);
        when(modelMapper.map(lesson, LessonResponseDto.class)).thenReturn(lessonResponseDto);
        when(s3StorageService.getAccessibleFileUrl("lessons/1/intro.mp4"))
                .thenReturn("accessible-file-url");

        LessonResponseDto result = lessonService.createLesson(100L, request, file);

        assertNotNull(result);
        assertEquals("Introduction", result.getTitle());
        assertEquals("accessible-file-url", result.getContentUrl());
        assertEquals(1L, result.getCourseId());

        verify(courseService).getCourseEntityById(1L);
        verify(s3StorageService).uploadFile(any(MultipartFile.class), eq("lessons/1"));
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    void createLesson_shouldCreateLessonWithoutFile() {
        Lesson savedLesson = Lesson.builder()
                .id(10L)
                .course(course)
                .title("Introduction")
                .contentType(ContentType.VIDEO)
                .contentUrl(null)
                .orderIndex(1)
                .isFreePreview(true)
                .durationSeconds(300)
                .build();

        LessonResponseDto mappedDto = new LessonResponseDto();
        mappedDto.setId(10L);
        mappedDto.setTitle("Introduction");
        mappedDto.setContentType(ContentType.VIDEO);
        mappedDto.setContentUrl(null);
        mappedDto.setOrderIndex(1);
        mappedDto.setIsFreePreview(true);
        mappedDto.setDurationSeconds(300);

        when(courseService.getCourseEntityById(1L)).thenReturn(course);
        when(lessonRepository.save(any(Lesson.class))).thenReturn(savedLesson);
        when(modelMapper.map(savedLesson, LessonResponseDto.class)).thenReturn(mappedDto);
        when(s3StorageService.getAccessibleFileUrl(null)).thenReturn(null);

        LessonResponseDto result = lessonService.createLesson(100L, request, null);

        assertNotNull(result);
        assertEquals("Introduction", result.getTitle());
        assertNull(result.getContentUrl());
        assertEquals(1L, result.getCourseId());

        verify(s3StorageService, never()).uploadFile(any(), any());
    }

    @Test
    void createLesson_shouldThrowException_whenInstructorDoesNotOwnCourse() {
        when(courseService.getCourseEntityById(1L)).thenReturn(course);

        assertThrows(UnauthorizedException.class,
                () -> lessonService.createLesson(999L, request, null));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void getLessonsByCourse_shouldReturnLessonsList() {
        when(lessonRepository.findByCourseIdOrderByOrderIndex(1L)).thenReturn(List.of(lesson));
        when(modelMapper.map(lesson, LessonResponseDto.class)).thenReturn(lessonResponseDto);
        when(s3StorageService.getAccessibleFileUrl("lessons/1/intro.mp4"))
                .thenReturn("accessible-file-url");

        List<LessonResponseDto> result = lessonService.getLessonsByCourse(1L, 200L, "STUDENT");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Introduction", result.get(0).getTitle());
        assertEquals("accessible-file-url", result.get(0).getContentUrl());
        assertEquals(1L, result.get(0).getCourseId());

        verify(lessonRepository).findByCourseIdOrderByOrderIndex(1L);
    }

    @Test
    void updateLesson_shouldUpdateLessonWithFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "updated.mp4",
                "video/mp4",
                "updated video".getBytes()
        );

        CreateLessonRequestDto updateRequest = new CreateLessonRequestDto();
        updateRequest.setTitle("Updated Lesson");
        updateRequest.setContentType(ContentType.VIDEO);
        updateRequest.setOrderIndex(2);
        updateRequest.setIsFreePreview(false);

        Lesson updatedLesson = Lesson.builder()
                .id(10L)
                .course(course)
                .title("Updated Lesson")
                .contentType(ContentType.VIDEO)
                .contentUrl("new-uploaded-url")
                .orderIndex(2)
                .isFreePreview(false)
                .durationSeconds(300)
                .build();

        LessonResponseDto updatedDto = new LessonResponseDto();
        updatedDto.setId(10L);
        updatedDto.setTitle("Updated Lesson");
        updatedDto.setContentType(ContentType.VIDEO);
        updatedDto.setContentUrl("new-uploaded-url");
        updatedDto.setOrderIndex(2);
        updatedDto.setIsFreePreview(false);
        updatedDto.setDurationSeconds(300);

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(s3StorageService.uploadFile(any(MultipartFile.class), eq("lessons/1")))
                .thenReturn("new-uploaded-url");
        when(lessonRepository.save(any(Lesson.class))).thenReturn(updatedLesson);
        when(modelMapper.map(updatedLesson, LessonResponseDto.class)).thenReturn(updatedDto);
        when(s3StorageService.getAccessibleFileUrl("new-uploaded-url"))
                .thenReturn("accessible-new-url");

        LessonResponseDto result = lessonService.updateLesson(10L, 100L, updateRequest, file);

        assertNotNull(result);
        assertEquals("Updated Lesson", result.getTitle());
        assertEquals("accessible-new-url", result.getContentUrl());
        assertEquals(1L, result.getCourseId());

        verify(s3StorageService).uploadFile(any(MultipartFile.class), eq("lessons/1"));
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    void updateLesson_shouldUpdateLessonWithoutFile() {
        CreateLessonRequestDto updateRequest = new CreateLessonRequestDto();
        updateRequest.setTitle("Updated Lesson");
        updateRequest.setContentType(ContentType.DOCUMENT);
        updateRequest.setOrderIndex(2);
        updateRequest.setIsFreePreview(false);

        Lesson updatedLesson = Lesson.builder()
                .id(10L)
                .course(course)
                .title("Updated Lesson")
                .contentType(ContentType.DOCUMENT)
                .contentUrl("lessons/1/intro.mp4")
                .orderIndex(2)
                .isFreePreview(false)
                .durationSeconds(300)
                .build();

        LessonResponseDto updatedDto = new LessonResponseDto();
        updatedDto.setId(10L);
        updatedDto.setTitle("Updated Lesson");
        updatedDto.setContentType(ContentType.DOCUMENT);
        updatedDto.setContentUrl("lessons/1/intro.mp4");
        updatedDto.setOrderIndex(2);
        updatedDto.setIsFreePreview(false);
        updatedDto.setDurationSeconds(300);

        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(updatedLesson);
        when(modelMapper.map(updatedLesson, LessonResponseDto.class)).thenReturn(updatedDto);
        when(s3StorageService.getAccessibleFileUrl("lessons/1/intro.mp4"))
                .thenReturn("accessible-file-url");

        LessonResponseDto result = lessonService.updateLesson(10L, 100L, updateRequest, null);

        assertNotNull(result);
        assertEquals("Updated Lesson", result.getTitle());
        assertEquals(ContentType.DOCUMENT, result.getContentType());
        assertEquals("accessible-file-url", result.getContentUrl());

        verify(s3StorageService, never()).uploadFile(any(), any());
    }

    @Test
    void updateLesson_shouldThrowException_whenInstructorNotOwner() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        assertThrows(UnauthorizedException.class,
                () -> lessonService.updateLesson(10L, 999L, request, null));

        verify(lessonRepository, never()).save(any(Lesson.class));
    }

    @Test
    void deleteLesson_shouldDeleteLessonAndChildren() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));
        doNothing().when(quizQuestionRepository).deleteByLessonId(10L);
        doNothing().when(lessonProgressRepository).deleteByLessonId(10L);
        doNothing().when(learningEventRepository).deleteByLessonId(10L);
        doNothing().when(lessonRepository).delete(lesson);

        lessonService.deleteLesson(10L, 100L);

        verify(quizQuestionRepository).deleteByLessonId(10L);
        verify(lessonProgressRepository).deleteByLessonId(10L);
        verify(learningEventRepository).deleteByLessonId(10L);
        verify(lessonRepository).delete(lesson);
    }

    @Test
    void deleteLesson_shouldThrowException_whenInstructorNotOwner() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        assertThrows(UnauthorizedException.class,
                () -> lessonService.deleteLesson(10L, 999L));

        verify(quizQuestionRepository, never()).deleteByLessonId(any(Long.class));
        verify(lessonRepository, never()).delete(any(Lesson.class));
    }

    @Test
    void getLessonEntityById_shouldReturnLesson() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.of(lesson));

        Lesson result = lessonService.getLessonEntityById(10L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Introduction", result.getTitle());
    }

    @Test
    void getLessonEntityById_shouldThrowException_whenLessonNotFound() {
        when(lessonRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> lessonService.getLessonEntityById(10L));
    }
}