package com.lms.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "quiz_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lesson_id", nullable = false)
	@ToString.Exclude
	private Lesson lesson;

	@Column(name = "question_text", nullable = false, length = 500)
	private String questionText;

	@Column(name = "option_a", nullable = false, length = 300)
	private String optionA;

	@Column(name = "option_b", nullable = false, length = 300)
	private String optionB;

	@Column(name = "option_c", nullable = false, length = 300)
	private String optionC;

	@Column(name = "option_d", nullable = false, length = 300)
	private String optionD;

	@Column(name = "correct_options", nullable = false, length = 20)
	private String correctOptions;

	@Column(name = "order_index", nullable = false)
	private Integer orderIndex;
}
