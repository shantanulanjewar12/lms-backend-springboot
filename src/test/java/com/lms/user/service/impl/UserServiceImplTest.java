package com.lms.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;

import com.lms.course.service.S3StorageService;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.request.UpdateProfileRequestDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private S3StorageService s3StorageService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Priya Nivalkar");
        user.setEmail("priya@gmail.com");
        user.setBio("Student");
        user.setProfilePic("profiles/pic.png");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.STUDENT);

        userResponseDto = new UserResponseDto();
        userResponseDto.setId(1L);
        userResponseDto.setFullName("Priya Nivalkar");
        userResponseDto.setEmail("priya@gmail.com");
        userResponseDto.setBio("Student");
        userResponseDto.setProfilePic("profiles/pic.png");
        userResponseDto.setStatus(UserStatus.ACTIVE);
        userResponseDto.setRole(UserRole.STUDENT);
    }

    @Test
    @DisplayName("getUserById should return user dto when user exists")
    void getUserById_shouldReturnUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(userResponseDto);
        when(s3StorageService.getAccessibleFileUrl("profiles/pic.png"))
                .thenReturn("https://bucket-url/profiles/pic.png");

        UserResponseDto result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Priya Nivalkar", result.getFullName());
        assertEquals("priya@gmail.com", result.getEmail());
        assertEquals("https://bucket-url/profiles/pic.png", result.getProfilePic());

        verify(userRepository).findById(1L);
        verify(modelMapper).map(user, UserResponseDto.class);
        verify(s3StorageService).getAccessibleFileUrl("profiles/pic.png");
    }

    @Test
    @DisplayName("getUserById should throw exception when user does not exist")
    void getUserById_shouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(1L));

        verify(userRepository).findById(1L);
        verify(modelMapper, never()).map(any(), eq(UserResponseDto.class));
    }

    @Test
    @DisplayName("getUserByEmail should return user dto when email exists")
    void getUserByEmail_shouldReturnUserDto() {
        when(userRepository.findByEmail("priya@gmail.com")).thenReturn(Optional.of(user));
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(userResponseDto);
        when(s3StorageService.getAccessibleFileUrl("profiles/pic.png"))
                .thenReturn("https://bucket-url/profiles/pic.png");

        UserResponseDto result = userService.getUserByEmail("priya@gmail.com");

        assertNotNull(result);
        assertEquals("priya@gmail.com", result.getEmail());
        assertEquals("https://bucket-url/profiles/pic.png", result.getProfilePic());

        verify(userRepository).findByEmail("priya@gmail.com");
    }

    @Test
    @DisplayName("getUserByEmail should throw exception when email does not exist")
    void getUserByEmail_shouldThrowException() {
        when(userRepository.findByEmail("priya@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByEmail("priya@gmail.com"));

        verify(userRepository).findByEmail("priya@gmail.com");
    }

    @Test
    @DisplayName("getUserEntityByEmail should return user entity when email exists")
    void getUserEntityByEmail_shouldReturnUser() {
        when(userRepository.findByEmail("priya@gmail.com")).thenReturn(Optional.of(user));

        User result = userService.getUserEntityByEmail("priya@gmail.com");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("priya@gmail.com", result.getEmail());

        verify(userRepository).findByEmail("priya@gmail.com");
    }

    @Test
    @DisplayName("getUserEntityByEmail should throw exception when email does not exist")
    void getUserEntityByEmail_shouldThrowException() {
        when(userRepository.findByEmail("priya@gmail.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserEntityByEmail("priya@gmail.com"));

        verify(userRepository).findByEmail("priya@gmail.com");
    }

    @Test
    @DisplayName("updateProfile should update fullName, bio and cleaned profilePicUrl")
    void updateProfile_shouldUpdateFields() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto();
        request.setFullName("Priya Updated");
        request.setBio("Updated bio");
        request.setProfilePicUrl("https://bucket-url/profiles/new-pic.png?X-Amz-Algorithm=test");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFullName("Priya Updated");
        savedUser.setEmail("priya@gmail.com");
        savedUser.setBio("Updated bio");
        savedUser.setProfilePic("https://bucket-url/profiles/new-pic.png");
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setRole(UserRole.STUDENT);

        UserResponseDto mappedDto = new UserResponseDto();
        mappedDto.setId(1L);
        mappedDto.setFullName("Priya Updated");
        mappedDto.setEmail("priya@gmail.com");
        mappedDto.setBio("Updated bio");
        mappedDto.setProfilePic("https://bucket-url/profiles/new-pic.png");
        mappedDto.setStatus(UserStatus.ACTIVE);
        mappedDto.setRole(UserRole.STUDENT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(savedUser, UserResponseDto.class)).thenReturn(mappedDto);
        when(s3StorageService.getAccessibleFileUrl("https://bucket-url/profiles/new-pic.png"))
                .thenReturn("https://bucket-url/profiles/new-pic.png");

        UserResponseDto result = userService.updateProfile(1L, request);

        assertNotNull(result);
        assertEquals("Priya Updated", result.getFullName());
        assertEquals("Updated bio", result.getBio());
        assertEquals("https://bucket-url/profiles/new-pic.png", result.getProfilePic());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile should update only non-null fields")
    void updateProfile_shouldUpdateOnlyNonNullFields() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto();
        request.setFullName("Only Name Updated");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFullName("Only Name Updated");
        savedUser.setEmail("priya@gmail.com");
        savedUser.setBio("Student");
        savedUser.setProfilePic("profiles/pic.png");
        savedUser.setStatus(UserStatus.ACTIVE);
        savedUser.setRole(UserRole.STUDENT);

        UserResponseDto mappedDto = new UserResponseDto();
        mappedDto.setId(1L);
        mappedDto.setFullName("Only Name Updated");
        mappedDto.setEmail("priya@gmail.com");
        mappedDto.setBio("Student");
        mappedDto.setProfilePic("profiles/pic.png");
        mappedDto.setStatus(UserStatus.ACTIVE);
        mappedDto.setRole(UserRole.STUDENT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(savedUser, UserResponseDto.class)).thenReturn(mappedDto);
        when(s3StorageService.getAccessibleFileUrl("profiles/pic.png"))
                .thenReturn("https://bucket-url/profiles/pic.png");

        UserResponseDto result = userService.updateProfile(1L, request);

        assertNotNull(result);
        assertEquals("Only Name Updated", result.getFullName());
        assertEquals("Student", result.getBio());

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile should throw exception when user does not exist")
    void updateProfile_shouldThrowException() {
        UpdateProfileRequestDto request = new UpdateProfileRequestDto();
        request.setFullName("Priya Updated");

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateProfile(1L, request));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("uploadProfilePicture should upload file and return accessible url")
    void uploadProfilePicture_shouldUploadAndReturnUrl() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "dummy-image".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(s3StorageService.uploadFile(file, "profiles")).thenReturn("profiles/uploaded.png");
        when(s3StorageService.getAccessibleFileUrl("profiles/uploaded.png"))
                .thenReturn("https://bucket-url/profiles/uploaded.png");
        when(userRepository.save(any(User.class))).thenReturn(user);

        String result = userService.uploadProfilePicture(1L, file);

        assertNotNull(result);
        assertEquals("https://bucket-url/profiles/uploaded.png", result);

        verify(userRepository).findById(1L);
        verify(s3StorageService).uploadFile(file, "profiles");
        verify(userRepository).save(any(User.class));
        verify(s3StorageService).getAccessibleFileUrl("profiles/uploaded.png");
    }

    @Test
    @DisplayName("uploadProfilePicture should throw exception when user does not exist")
    void uploadProfilePicture_shouldThrowException() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "profile.png",
                "image/png",
                "dummy-image".getBytes()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.uploadProfilePicture(1L, file));

        verify(userRepository).findById(1L);
        verify(s3StorageService, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("getAllUsers should return paged dto response")
    void getAllUsers_shouldReturnPage() {
        Page<User> userPage = new PageImpl<>(
                List.of(user),
                PageRequest.of(0, 10),
                1
        );

        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(modelMapper.map(user, UserResponseDto.class)).thenReturn(userResponseDto);
        when(s3StorageService.getAccessibleFileUrl("profiles/pic.png"))
                .thenReturn("https://bucket-url/profiles/pic.png");

        Page<UserResponseDto> result = userService.getAllUsers(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Priya Nivalkar", result.getContent().get(0).getFullName());
        assertEquals("https://bucket-url/profiles/pic.png", result.getContent().get(0).getProfilePic());

        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("updateUserStatus should update status and return dto")
    void updateUserStatus_shouldReturnUpdatedUser() {
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setFullName("Priya Nivalkar");
        savedUser.setEmail("priya@gmail.com");
        savedUser.setBio("Student");
        savedUser.setProfilePic("profiles/pic.png");
        savedUser.setStatus(UserStatus.SUSPENDED);
        savedUser.setRole(UserRole.STUDENT);

        UserResponseDto mappedDto = new UserResponseDto();
        mappedDto.setId(1L);
        mappedDto.setFullName("Priya Nivalkar");
        mappedDto.setEmail("priya@gmail.com");
        mappedDto.setBio("Student");
        mappedDto.setProfilePic("profiles/pic.png");
        mappedDto.setStatus(UserStatus.SUSPENDED);
        mappedDto.setRole(UserRole.STUDENT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(modelMapper.map(savedUser, UserResponseDto.class)).thenReturn(mappedDto);
        when(s3StorageService.getAccessibleFileUrl("profiles/pic.png"))
                .thenReturn("https://bucket-url/profiles/pic.png");

        UserResponseDto result = userService.updateUserStatus(1L, UserStatus.SUSPENDED);

        assertNotNull(result);
        assertEquals(UserStatus.SUSPENDED, result.getStatus());

        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUserStatus should throw exception when user does not exist")
    void updateUserStatus_shouldThrowException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUserStatus(1L, UserStatus.SUSPENDED));

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deleteUser should delete user when id exists")
    void deleteUser_shouldDeleteSuccessfully() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> userService.deleteUser(1L));

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser should throw exception when id does not exist")
    void deleteUser_shouldThrowException() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(1L));

        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(1L);
    }
}