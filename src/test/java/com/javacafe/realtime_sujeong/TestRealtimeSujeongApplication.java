package com.javacafe.realtime_sujeong;

import org.springframework.boot.SpringApplication;

public class TestRealtimeSujeongApplication {

	public static void main(String[] args) {
		SpringApplication.from(RealtimeSujeongApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
