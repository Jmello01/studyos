package com.studyos;

import org.springframework.boot.SpringApplication;

public class TestStudyosApplication {

	public static void main(String[] args) {
		SpringApplication.from(StudyosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
