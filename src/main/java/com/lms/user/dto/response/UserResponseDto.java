package com.lms.user.dto.response;

import java.time.LocalDateTime;

import com.lms.user.vo.UserRole;
import com.lms.user.vo.UserStatus;

import lombok.Data;

@Data
public class UserResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private UserRole role;
    private UserStatus status;
    private String profilePic;
    private String bio;
    private LocalDateTime createdAt;
}