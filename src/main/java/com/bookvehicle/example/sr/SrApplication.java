package com.bookvehicle.example.sr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class    SrApplication {

	public static void main(String[] args) {
		SpringApplication.run(SrApplication.class, args);
	}

}
