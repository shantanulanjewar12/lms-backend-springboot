package com.lms.user.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequestDto {
    private String fullName;
    private String bio;
    private String profilePicUrl;
}