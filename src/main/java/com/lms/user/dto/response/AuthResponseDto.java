package com.lms.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
	
    private String accessToken;
    @Builder.Default private String tokenType = "Bearer";
    @Builder.Default private long expiresIn = 86400000L;
    private UserResponseDto user;
    
}