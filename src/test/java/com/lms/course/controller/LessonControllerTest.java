package com.lms.course.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.course.dto.request.CreateLessonRequestDto;
import com.lms.course.dto.request.QuizAnswerItemRequestDto;
import com.lms.course.dto.request.QuizQuestionUpsertRequestDto;
import com.lms.course.dto.request.QuizSubmitRequestDto;
import com.lms.course.dto.response.LessonResponseDto;
import com.lms.course.dto.response.QuizQuestionResponseDto;
import com.lms.course.dto.response.QuizSubmitResponseDto;
import com.lms.course.service.LessonService;
import com.lms.course.service.QuizService;
import com.lms.course.vo.ContentType;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    @Mock
    private LessonService lessonService;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private LessonController lessonController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private LessonResponseDto lessonResponseDto;
    private QuizQuestionResponseDto quizQuestionResponseDto;
    private QuizSubmitResponseDto quizSubmitResponseDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lessonController).build();
        objectMapper = new ObjectMapper();

        lessonResponseDto = new LessonResponseDto();
        lessonResponseDto.setId(1L);
        lessonResponseDto.setCourseId(101L);
        lessonResponseDto.setTitle("Introduction");
        lessonResponseDto.setContentType(ContentType.VIDEO);
        lessonResponseDto.setContentUrl("https://s3.aws.com/lesson.mp4");
        lessonResponseDto.setOrderIndex(1);
        lessonResponseDto.setIsFreePreview(true);
        lessonResponseDto.setDurationSeconds(300);

        quizQuestionResponseDto = new QuizQuestionResponseDto();
        quizQuestionResponseDto.setId(11L);
        quizQuestionResponseDto.setLessonId(1L);
        quizQuestionResponseDto.setQuestionText("What is Java?");
        quizQuestionResponseDto.setOptionA("Language");
        quizQuestionResponseDto.setOptionB("Database");
        quizQuestionResponseDto.setOptionC("OS");
        quizQuestionResponseDto.setOptionD("IDE");

        quizSubmitResponseDto = new QuizSubmitResponseDto();
        quizSubmitResponseDto.setLessonId(1L);
        quizSubmitResponseDto.setCorrectAnswers(1);
        quizSubmitResponseDto.setTotalQuestions(1);
        quizSubmitResponseDto.setScorePercent(100.0);
        quizSubmitResponseDto.setPassed(true);
    }

    @Test
    void createLesson_shouldReturnCreated() throws Exception {
        CreateLessonRequestDto request = new CreateLessonRequestDto();
        request.setCourseId(101L);
        request.setTitle("Introduction");
        request.setContentType(ContentType.VIDEO);
        request.setOrderIndex(1);
        request.setIsFreePreview(true);
        request.setDurationSeconds(300);

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "lesson.mp4",
                "video/mp4",
                "dummy video".getBytes()
        );

        when(lessonService.createLesson(eq(1L), any(CreateLessonRequestDto.class), any()))
                .thenReturn(lessonResponseDto);

        mockMvc.perform(multipart("/api/v1/lessons")
                        .file(dataPart)
                        .file(filePart)
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson created"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Introduction"));
    }

    @Test
    void getLessons_shouldReturnLessons_whenAuthenticated() throws Exception {
        when(lessonService.getLessonsByCourse(101L, 1L, "ROLE_STUDENT"))
                .thenReturn(List.of(lessonResponseDto));

        mockMvc.perform(get("/api/v1/courses/101/lessons")
                        .principal(createAuth(1L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lessons fetched"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Introduction"));
    }

    @Test
    void getLessons_shouldReturnLessons_whenPublic() throws Exception {
        when(lessonService.getLessonsByCourse(101L, null, "PUBLIC"))
                .thenReturn(List.of(lessonResponseDto));

        mockMvc.perform(get("/api/v1/courses/101/lessons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void updateLesson_shouldReturnUpdatedLesson() throws Exception {
        CreateLessonRequestDto request = new CreateLessonRequestDto();
        request.setCourseId(101L);
        request.setTitle("Updated Introduction");
        request.setContentType(ContentType.VIDEO);
        request.setOrderIndex(1);
        request.setIsFreePreview(false);
        request.setDurationSeconds(400);

        LessonResponseDto updated = new LessonResponseDto();
        updated.setId(1L);
        updated.setCourseId(101L);
        updated.setTitle("Updated Introduction");
        updated.setContentType(ContentType.VIDEO);

        MockMultipartFile dataPart = new MockMultipartFile(
                "data",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "updated.mp4",
                "video/mp4",
                "updated video".getBytes()
        );

        when(lessonService.updateLesson(eq(1L), eq(1L), any(CreateLessonRequestDto.class), any()))
                .thenReturn(updated);

        mockMvc.perform(multipart("/api/v1/lessons/1")
                        .file(dataPart)
                        .file(filePart)
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR"))
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson updated"))
                .andExpect(jsonPath("$.data.title").value("Updated Introduction"));
    }

    @Test
    void deleteLesson_shouldReturnSuccess() throws Exception {
        doNothing().when(lessonService).deleteLesson(1L, 1L);

        mockMvc.perform(delete("/api/v1/lessons/1")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Lesson deleted"));
    }

    @Test
    void upsertQuizQuestions_shouldReturnSavedQuestions() throws Exception {
        QuizQuestionUpsertRequestDto request = new QuizQuestionUpsertRequestDto();
        request.setQuestionText("What is Java?");
        request.setOptionA("Language");
        request.setOptionB("Database");
        request.setOptionC("OS");
        request.setOptionD("IDE");
        request.setCorrectOptions(List.of("A"));
        request.setOrderIndex(1);

        when(quizService.upsertQuizQuestions(eq(1L), eq(1L), any()))
                .thenReturn(List.of(quizQuestionResponseDto));

        mockMvc.perform(put("/api/v1/lessons/1/quiz")
                        .principal(createAuth(1L, "ROLE_INSTRUCTOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Quiz questions saved"))
                .andExpect(jsonPath("$.data[0].id").value(11))
                .andExpect(jsonPath("$.data[0].questionText").value("What is Java?"));
    }

    @Test
    void getQuizQuestions_shouldReturnQuestions() throws Exception {
        when(quizService.getQuizQuestions(1L, 1L, "ROLE_STUDENT"))
                .thenReturn(List.of(quizQuestionResponseDto));

        mockMvc.perform(get("/api/v1/lessons/1/quiz")
                        .principal(createAuth(1L, "ROLE_STUDENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Quiz questions fetched"))
                .andExpect(jsonPath("$.data[0].id").value(11));
    }

    @Test
    void submitQuiz_shouldReturnResult() throws Exception {
        QuizAnswerItemRequestDto answer = new QuizAnswerItemRequestDto();
        answer.setQuestionId(11L);
        answer.setSelectedOptions(List.of("A"));

        QuizSubmitRequestDto request = new QuizSubmitRequestDto();
        request.setAnswers(List.of(answer));

        when(quizService.submitQuiz(eq(1L), eq(1L), any(QuizSubmitRequestDto.class)))
                .thenReturn(quizSubmitResponseDto);

        mockMvc.perform(post("/api/v1/lessons/1/quiz/submit")
                        .principal(createAuth(1L, "ROLE_STUDENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Quiz submitted"))
                .andExpect(jsonPath("$.data.scorePercent").value(100.0))
                .andExpect(jsonPath("$.data.passed").value(true));
    }

    @Test
    void createLesson_shouldThrowException_whenAuthenticationIsNull() {
        CreateLessonRequestDto request = new CreateLessonRequestDto();

        assertThrows(NullPointerException.class,
                () -> lessonController.createLesson(null, request, null));
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