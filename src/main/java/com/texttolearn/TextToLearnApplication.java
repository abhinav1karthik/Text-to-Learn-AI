package com.texttolearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TextToLearnApplication {

	public static void main(String[] args) {
		SpringApplication.run(TextToLearnApplication.class, args);
	}

}
