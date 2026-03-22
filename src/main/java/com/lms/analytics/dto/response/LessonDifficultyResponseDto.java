package com.lms.analytics.dto.response;


import com.lms.analytics.vo.DifficultyLevel;

import lombok.Data;
@Data
public class LessonDifficultyResponseDto {
    private Long lessonId;
    private String lessonTitle;
    private Double difficultyScore;
    private DifficultyLevel difficultyLevel;
    private Double quizFailureRate;
    private Double rewindRate;
    private Double dropOffRate;
}