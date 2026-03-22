package com.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiniLmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniLmsApplication.class, args);
	}

}
