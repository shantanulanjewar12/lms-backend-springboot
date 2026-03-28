package com.lms.course.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.lms.course.dto.request.SubmitReviewRequestDto;
import com.lms.course.dto.response.ReviewResponseDto;
import com.lms.course.service.ReviewService;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController reviewController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private ReviewResponseDto reviewResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();
        objectMapper = new ObjectMapper();

        reviewResponseDto = new ReviewResponseDto();
        reviewResponseDto.setId(11L);
        reviewResponseDto.setCourseId(101L);
        reviewResponseDto.setRating((short) 5);
        reviewResponseDto.setComment("Excellent course");
        reviewResponseDto.setStudentName("Priya");
    }

    @Test
    void submitReview_shouldReturnCreated() throws Exception {
        SubmitReviewRequestDto request = new SubmitReviewRequestDto();
        request.setRating((short) 5);
        request.setComment("Excellent course");

        when(reviewService.submitReview(eq(101L), eq(1L), any(SubmitReviewRequestDto.class)))
                .thenReturn(reviewResponseDto);

        mockMvc.perform(post("/api/v1/courses/101/reviews")
                        .principal(createAuth(1L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review submitted"))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.data.courseId").value(101))
                .andExpect(jsonPath("$.data.comment").value("Excellent course"));
    }

    @Test
    void getReviews_shouldReturnList() throws Exception {
        when(reviewService.getReviewsByCourse(101L))
                .thenReturn(List.of(reviewResponseDto));

        mockMvc.perform(get("/api/v1/courses/101/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reviews fetched"))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].studentName").value("Priya"));
    }

    @Test
    void getAverageRating_shouldReturnValue() throws Exception {
        when(reviewService.getAverageRating(101L)).thenReturn(4.7);

        mockMvc.perform(get("/api/v1/courses/101/reviews/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Average rating"))
                .andExpect(jsonPath("$.data").value(4.7));
    }

    @Test
    void deleteReview_shouldReturnSuccess() throws Exception {
        doNothing().when(reviewService).deleteReview(11L);

        mockMvc.perform(delete("/api/v1/courses/101/reviews/11")
                        .principal(createAuth(99L, "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review deleted"));
    }

    @Test
    void submitReview_shouldThrowException_whenAuthenticationIsNull() {
        SubmitReviewRequestDto request = new SubmitReviewRequestDto();

        assertThrows(NullPointerException.class,
                () -> reviewController.submitReview(101L, null, request));
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
