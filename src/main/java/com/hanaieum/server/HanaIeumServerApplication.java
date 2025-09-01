package com.hanaieum.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class HanaIeumServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HanaIeumServerApplication.class, args);
    }

}
