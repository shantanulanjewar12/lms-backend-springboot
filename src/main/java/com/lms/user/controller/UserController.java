package com.lms.user.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lms.shared.dto.ApiResponse;
import com.lms.shared.jwt.JwtUtil;
import com.lms.user.dto.request.UpdateProfileRequestDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.service.UserService;
import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;
	private final JwtUtil jwtUtil;

	
	
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponseDto>> getMyProfile(Authentication auth) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Profile fetched", userService.getUserById(userId)));
	}
	
	

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<UserResponseDto>> updateMyProfile(Authentication auth,
			@RequestBody UpdateProfileRequestDto request) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(userId, request)));
	}

	
	
	
	@PostMapping("/me/profile-picture")
	public ResponseEntity<ApiResponse<String>> uploadProfilePicture(Authentication auth,
			@RequestParam("file") MultipartFile file) {
		Long userId = (Long) auth.getCredentials();
		return ResponseEntity
				.ok(ApiResponse.success("Profile picture uploaded", userService.uploadProfilePicture(userId, file)));
	}

	
	
	@GetMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("User fetched", userService.getUserById(id)));
	}
	
	

	@GetMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getAllUsers(
			@RequestParam(required = false) String search,
			@RequestParam(required = false) UserRole role,
			@PageableDefault(size = 10) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Users fetched", userService.getAllUsers(pageable, search, role)));
	}
	
	
	

	@PutMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<UserResponseDto>> updateStatus(@PathVariable Long id,
			@RequestParam UserStatus status) {
		return ResponseEntity.ok(ApiResponse.success("Status updated", userService.updateUserStatus(id, status)));
	}

	
	
	
	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
		userService.deleteUser(id);
		return ResponseEntity.ok(ApiResponse.success("User deleted"));
	}
	
	
}