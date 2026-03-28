package com.lms.course.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.course.dto.request.CreateCourseRequestDto;
import com.lms.course.dto.request.UpdateCourseRequestDto;
import com.lms.course.dto.response.CourseResponseDto;
import com.lms.course.dto.response.CourseSummaryResponseDto;
import com.lms.course.service.CourseService;
import com.lms.course.vo.CourseLevel;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private CourseService courseService;

    @InjectMocks
    private CourseController courseController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private CourseResponseDto courseResponseDto;
    private CourseSummaryResponseDto courseSummaryResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(courseController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        objectMapper = new ObjectMapper();

        courseResponseDto = new CourseResponseDto();
        courseResponseDto.setId(101L);
        courseResponseDto.setInstructorId(1L);
        courseResponseDto.setTitle("Java Masterclass");
        courseResponseDto.setDescription("Complete Java course");
        courseResponseDto.setPrice(BigDecimal.valueOf(999));
        courseResponseDto.setCurrency("INR");
        courseResponseDto.setCategory("Programming");
        courseResponseDto.setThumbnailUrl("thumb.jpg");

        courseSummaryResponseDto = new CourseSummaryResponseDto();
        courseSummaryResponseDto.setId(101L);
        courseSummaryResponseDto.setTitle("Java Masterclass");
        courseSummaryResponseDto.setPrice(BigDecimal.valueOf(999));
        courseSummaryResponseDto.setCategory("Programming");
        courseSummaryResponseDto.setThumbnailUrl("thumb.jpg");
    }

    @Test
    void createCourse_shouldReturnCreated() throws Exception {
        CreateCourseRequestDto request = new CreateCourseRequestDto();
        request.setTitle("Java Masterclass");
        request.setDescription("Complete Java course");
        request.setPrice(BigDecimal.valueOf(999));
        request.setCurrency("INR");
        request.setCategory("Programming");
        request.setLevel(CourseLevel.BEGINNER);

        when(courseService.createCourse(eq(1L), any(CreateCourseRequestDto.class)))
                .thenReturn(courseResponseDto);

        mockMvc.perform(post("/api/v1/courses")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course created"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.title").value("Java Masterclass"));
    }

    @Test
    void getPublishedCourses_shouldReturnPage() throws Exception {
        Page<CourseSummaryResponseDto> page =
                new PageImpl<>(List.of(courseSummaryResponseDto), PageRequest.of(0, 10), 1);

        when(courseService.getPublishedCourses(eq("Programming"), eq("beginner"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/courses")
                        .param("category", "Programming")
                        .param("level", "beginner")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Courses fetched"))
                .andExpect(jsonPath("$.data.content[0].id").value(101))
                .andExpect(jsonPath("$.data.content[0].title").value("Java Masterclass"));
    }

    @Test
    void getMyCourses_shouldReturnInstructorCourses() throws Exception {
        Page<CourseSummaryResponseDto> page =
                new PageImpl<>(List.of(courseSummaryResponseDto), PageRequest.of(0, 10), 1);

        when(courseService.getInstructorCourses(eq(1L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/courses/instructor/my")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR"))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Instructor courses fetched"))
                .andExpect(jsonPath("$.data.content[0].id").value(101));
    }

    @Test
    void getCourse_shouldReturnCourse() throws Exception {
        when(courseService.getCourseById(101L)).thenReturn(courseResponseDto);

        mockMvc.perform(get("/api/v1/courses/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course fetched"))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.title").value("Java Masterclass"));
    }

    @Test
    void updateCourse_shouldReturnUpdatedCourse() throws Exception {
        UpdateCourseRequestDto request = new UpdateCourseRequestDto();
        request.setTitle("Updated Java Masterclass");

        CourseResponseDto updated = new CourseResponseDto();
        updated.setId(101L);
        updated.setTitle("Updated Java Masterclass");

        when(courseService.updateCourse(eq(101L), eq(1L), any(UpdateCourseRequestDto.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/courses/101")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course updated"))
                .andExpect(jsonPath("$.data.title").value("Updated Java Masterclass"));
    }

    @Test
    void deleteCourse_shouldReturnSuccess() throws Exception {
        doNothing().when(courseService).deleteCourse(101L, 1L, "ROLE_INSTRUCTOR");

        mockMvc.perform(delete("/api/v1/courses/101")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course deleted"));
    }

    @Test
    void publishCourse_shouldReturnPublishedCourse() throws Exception {
        CourseResponseDto published = new CourseResponseDto();
        published.setId(101L);
        published.setTitle("Java Masterclass");

        when(courseService.publishCourse(101L, 1L)).thenReturn(published);

        mockMvc.perform(patch("/api/v1/courses/101/publish")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Course published"))
                .andExpect(jsonPath("$.data.id").value(101));
    }

    @Test
    void uploadThumbnail_shouldReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "thumb.jpg",
                "image/jpeg",
                "dummy image".getBytes()
        );

        when(courseService.uploadThumbnail(eq(101L), eq(1L), any()))
                .thenReturn("https://s3.aws.com/thumb.jpg");

        mockMvc.perform(multipart("/api/v1/courses/101/thumbnail")
                        .file(file)
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Thumbnail uploaded"))
                .andExpect(jsonPath("$.data").value("https://s3.aws.com/thumb.jpg"));
    }

    @Test
    void createCourse_shouldThrowException_whenAuthenticationIsNull() {
        CreateCourseRequestDto request = new CreateCourseRequestDto();

        assertThrows(NullPointerException.class,
                () -> courseController.createCourse(null, request));
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