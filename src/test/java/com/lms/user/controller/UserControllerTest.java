package com.lms.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.shared.jwt.JwtUtil;
import com.lms.user.dto.request.UpdateProfileRequestDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();
    }

    private UserResponseDto buildUser() {
        UserResponseDto user = new UserResponseDto();
        user.setId(1L);
        user.setFullName("Priya Nivalkar");
        user.setEmail("priya@gmail.com");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return logged in user profile")
    void getMyProfile_shouldReturnProfile() throws Exception {
        UserResponseDto user = buildUser();
        when(userService.getUserById(1L)).thenReturn(user);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", 1L);

        mockMvc.perform(get("/api/v1/users/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile fetched"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("priya@gmail.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me should update logged in user profile")
    void updateMyProfile_shouldReturnUpdatedProfile() throws Exception {
        UserResponseDto updatedUser = buildUser();
        updatedUser.setFullName("Priya Updated");

        when(userService.updateProfile(anyLong(), any(UpdateProfileRequestDto.class)))
                .thenReturn(updatedUser);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", 1L);

        String json = """
            {
              "fullName": "Priya Updated",
              "bio": "Updated bio"
            }
            """;

        mockMvc.perform(put("/api/v1/users/me")
                .principal(auth)
                .contentType(APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated"))
                .andExpect(jsonPath("$.data.fullName").value("Priya Updated"));
    }

    @Test
    @DisplayName("POST /api/v1/users/me/profile-picture should upload file")
    void uploadProfilePicture_shouldReturnUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "dummy-image".getBytes()
        );

        when(userService.uploadProfilePicture(eq(1L), any()))
                .thenReturn("https://example.com/profile.png");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user", 1L);

        mockMvc.perform(multipart("/api/v1/users/me/profile-picture")
                .file(file)
                .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile picture uploaded"))
                .andExpect(jsonPath("$.data").value("https://example.com/profile.png"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return user by id")
    void getUserById_shouldReturnUser() throws Exception {
        UserResponseDto user = buildUser();
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User fetched"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/users should return paged users")
    void getAllUsers_shouldReturnPage() throws Exception {
        UserResponseDto user = buildUser();

        Page<UserResponseDto> page =
                new PageImpl<>(List.of(user),
                        org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/users")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users fetched"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("priya@gmail.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/status should update user status")
    void updateStatus_shouldReturnUpdatedUser() throws Exception {
        UserResponseDto user = buildUser();
        user.setStatus(UserStatus.SUSPENDED);

        when(userService.updateUserStatus(1L, UserStatus.SUSPENDED)).thenReturn(user);

        mockMvc.perform(put("/api/v1/users/1/status")
                .param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Status updated"))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} should delete user")
    void deleteUser_shouldReturnSuccess() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted"));
    }
}