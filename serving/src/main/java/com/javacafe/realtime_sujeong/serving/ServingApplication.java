package com.javacafe.realtime_sujeong.serving;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {"com.javacafe.realtime_sujeong.serving", "com.javacafe.realtime_sujeong.common"})
@EnableJpaAuditing
public class ServingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServingApplication.class, args);
	}

}