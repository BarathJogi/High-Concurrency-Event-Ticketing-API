package com.barath.ticketingapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TicketingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketingApiApplication.class, args);
	}

}
