package com.collectingwithzak;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CollectingWithZakApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectingWithZakApplication.class, args);
    }
}
