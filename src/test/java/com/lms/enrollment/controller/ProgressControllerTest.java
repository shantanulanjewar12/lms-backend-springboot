package com.lms.enrollment.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.enrollment.dto.request.LearningEventRequestDto;
import com.lms.enrollment.dto.request.LessonProgressRequestDto;
import com.lms.enrollment.dto.response.LearningEventResponseDto;
import com.lms.enrollment.dto.response.ProgressResponseDto;
import com.lms.enrollment.entity.LearningEvent;
import com.lms.enrollment.repository.LearningEventRepository;
import com.lms.enrollment.service.ProgressService;
import com.lms.enrollment.vo.LearningEventType;

@ExtendWith(MockitoExtension.class)
class ProgressControllerTest {

    @Mock
    private ProgressService progressService;

    @Mock
    private LearningEventRepository learningEventRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProgressController progressController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ProgressResponseDto progressResponseDto;
    private LearningEvent learningEvent;
    private LearningEventResponseDto learningEventResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(progressController).build();
        objectMapper = new ObjectMapper();

        progressResponseDto = new ProgressResponseDto();
        progressResponseDto.setCourseId(101L);
        progressResponseDto.setCompletionPercentage(50.0);

        learningEvent = LearningEvent.builder()
                .id(1L)
                .studentId(10L)
                .lessonId(5L)
                .eventType(LearningEventType.VIDEO_WATCH)
                .value(BigDecimal.valueOf(120))
                .build();

        learningEventResponseDto = new LearningEventResponseDto();
        learningEventResponseDto.setId(1L);
        learningEventResponseDto.setStudentId(10L);
        learningEventResponseDto.setLessonId(5L);
        learningEventResponseDto.setEventType(LearningEventType.VIDEO_WATCH);
        learningEventResponseDto.setValue(BigDecimal.valueOf(120));
    }

    @Test
    void updateProgress_shouldReturnSuccess() throws Exception {
        LessonProgressRequestDto request = new LessonProgressRequestDto();
        request.setLessonId(5L);
        request.setIsCompleted(true);
        request.setWatchDurationSeconds(120);

        when(progressService.updateLessonProgress(eq(10L), any(LessonProgressRequestDto.class)))
                .thenReturn(progressResponseDto);

        mockMvc.perform(put("/api/v1/progress/lesson")
                        .principal(createAuth(10L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Progress updated"))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.completionPercentage").value(50.0));
    }

    @Test
    void getCourseProgress_shouldReturnSuccess() throws Exception {
        when(progressService.getCourseProgress(10L, 101L))
                .thenReturn(progressResponseDto);

        mockMvc.perform(get("/api/v1/progress/course/101")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Progress fetched"))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.completionPercentage").value(50.0));
    }

    @Test
    void logEvent_shouldReturnCreated() throws Exception {
        LearningEventRequestDto request = new LearningEventRequestDto();
        request.setLessonId(5L);
        request.setEventType(LearningEventType.VIDEO_WATCH);
        request.setValue(BigDecimal.valueOf(120));

        when(learningEventRepository.save(any(LearningEvent.class))).thenReturn(learningEvent);
        when(modelMapper.map(learningEvent, LearningEventResponseDto.class)).thenReturn(learningEventResponseDto);

        mockMvc.perform(post("/api/v1/events")
                        .principal(createAuth(10L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Event logged"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.lessonId").value(5));
    }

    @Test
    void getEventsByLesson_shouldReturnList() throws Exception {
        when(learningEventRepository.findByLessonId(5L)).thenReturn(List.of(learningEvent));
        when(modelMapper.map(learningEvent, LearningEventResponseDto.class)).thenReturn(learningEventResponseDto);

        mockMvc.perform(get("/api/v1/events/lesson/5")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Events fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].lessonId").value(5));
    }

    @Test
    void updateProgress_shouldThrowException_whenAuthenticationIsNull() {
        LessonProgressRequestDto request = new LessonProgressRequestDto();

        assertThrows(NullPointerException.class,
                () -> progressController.updateProgress(null, request));
    }

    private Authentication createAuth(Long userId, String role) {
        return new Authentication() {
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of(new SimpleGrantedAuthority(role));
            }

            @Override
            public Object getCredentials() {
                return userId;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return "user";
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return "user";
            }
        };
    }
}
