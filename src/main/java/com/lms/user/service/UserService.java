package com.lms.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.lms.user.dto.request.UpdateProfileRequestDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.entity.User;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

public interface UserService {
	UserResponseDto getUserById(Long id);

	UserResponseDto getUserByEmail(String email);

	User getUserEntityByEmail(String email);

	UserResponseDto updateProfile(Long id, UpdateProfileRequestDto request);

	String uploadProfilePicture(Long id, MultipartFile file);

	Page<UserResponseDto> getAllUsers(Pageable pageable, String search, UserRole role);

	UserResponseDto updateUserStatus(Long id, UserStatus status);

	void deleteUser(Long id);
}