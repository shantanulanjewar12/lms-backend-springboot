package com.lms.enrollment.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.enrollment.dto.request.EnrollRequestDto;
import com.lms.enrollment.dto.response.EnrollmentResponseDto;
import com.lms.enrollment.service.EnrollmentService;
import com.lms.enrollment.vo.EnrollmentStatus;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentControllerTest {

    @Mock
    private EnrollmentService enrollmentService;

    @InjectMocks
    private EnrollmentController enrollmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private EnrollmentResponseDto enrollmentResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(enrollmentController).build();
        objectMapper = new ObjectMapper();

        enrollmentResponseDto = new EnrollmentResponseDto();
        enrollmentResponseDto.setId(1L);
        enrollmentResponseDto.setStudentId(10L);
        enrollmentResponseDto.setCourseId(101L);
        enrollmentResponseDto.setCourseTitle("Java Masterclass");
        enrollmentResponseDto.setCourseThumbnailUrl("thumb.jpg");
        enrollmentResponseDto.setStatus(EnrollmentStatus.ACTIVE);
        enrollmentResponseDto.setCompletionPercentage(20.0);
    }

    @Test
    void enroll_shouldReturnCreated() throws Exception {
        EnrollRequestDto request = new EnrollRequestDto();
        request.setCourseId(101L);

        when(enrollmentService.enroll(eq(10L), any(EnrollRequestDto.class)))
                .thenReturn(enrollmentResponseDto);

        mockMvc.perform(post("/api/v1/enrollments")
                        .principal(createAuth(10L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Enrolled successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.courseTitle").value("Java Masterclass"));
    }

    @Test
    void getMyEnrollments_shouldReturnList() throws Exception {
        when(enrollmentService.getMyEnrollments(10L))
                .thenReturn(List.of(enrollmentResponseDto));

        mockMvc.perform(get("/api/v1/enrollments/my")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Enrollments fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(101))
                .andExpect(jsonPath("$.data[0].courseTitle").value("Java Masterclass"));
    }

    @Test
    void checkEnrollment_shouldReturnTrue() throws Exception {
        when(enrollmentService.isEnrolled(10L, 101L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/enrollments/course/101/check")
                        .principal(createAuth(10L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Enrollment check"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getCourseStudents_shouldReturnList() throws Exception {
        when(enrollmentService.getStudentsByCourse(101L))
                .thenReturn(List.of(enrollmentResponseDto));

        mockMvc.perform(get("/api/v1/enrollments/course/101/students")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Students fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].studentId").value(10));
    }

    @Test
    void enroll_shouldThrowException_whenAuthenticationIsNull() {
        EnrollRequestDto request = new EnrollRequestDto();

        assertThrows(NullPointerException.class,
                () -> enrollmentController.enroll(null, request));
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