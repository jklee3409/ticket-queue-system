package com.mycom.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketQueueApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketQueueApplication.class, args);
    }

}
