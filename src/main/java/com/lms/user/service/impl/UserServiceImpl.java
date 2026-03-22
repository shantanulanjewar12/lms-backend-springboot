package com.lms.user.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lms.course.service.S3StorageService;
import com.lms.shared.exception.ResourceNotFoundException;
import com.lms.user.dto.request.UpdateProfileRequestDto;
import com.lms.user.dto.response.UserResponseDto;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.user.service.UserService;
import com.lms.user.vo.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final ModelMapper modelMapper;
	private final S3StorageService s3StorageService;

	@Override
	public UserResponseDto getUserById(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
		return modelMapper.map(user, UserResponseDto.class);
	}

	@Override
	public UserResponseDto getUserByEmail(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
		return modelMapper.map(user, UserResponseDto.class);
	}

	@Override
	public User getUserEntityByEmail(String email) {
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
	}

	@Override
	public UserResponseDto updateProfile(Long id, UpdateProfileRequestDto request) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
		if (request.getFullName() != null)
			user.setFullName(request.getFullName());
		if (request.getBio() != null)
			user.setBio(request.getBio());
		if (request.getProfilePicUrl() != null)
			user.setProfilePic(request.getProfilePicUrl());
		return modelMapper.map(userRepository.save(user), UserResponseDto.class);
	}

	@Override
	public String uploadProfilePicture(Long id, MultipartFile file) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
		String url = s3StorageService.uploadFile(file, "profiles");
		user.setProfilePic(url);
		userRepository.save(user);
		return url;
	}

	@Override
	public Page<UserResponseDto> getAllUsers(Pageable pageable) {
		return userRepository.findAll(pageable).map(u -> modelMapper.map(u, UserResponseDto.class));
	}

	@Override
	public UserResponseDto updateUserStatus(Long id, UserStatus status) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
		user.setStatus(status);
		return modelMapper.map(userRepository.save(user), UserResponseDto.class);
	}

	@Override
	public void deleteUser(Long id) {
		if (!userRepository.existsById(id))
			throw new ResourceNotFoundException("User", id);
		userRepository.deleteById(id);
	}
}