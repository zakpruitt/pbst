package com.zakpruitt.pbst;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class PbstApplication {

	public static void main(String[] args) {
		System.out.println("hello");
		SpringApplication.run(PbstApplication.class, args);
	}

}
