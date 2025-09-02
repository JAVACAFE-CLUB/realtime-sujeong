package com.javacafe.realtime_sujeong.indexer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.javacafe.realtime_sujeong.indexer", "com.javacafe.realtime_sujeong.common"})
public class IndexerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IndexerApplication.class, args);
	}

}