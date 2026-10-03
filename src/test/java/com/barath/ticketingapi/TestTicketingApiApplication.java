package com.barath.ticketingapi;

import org.springframework.boot.SpringApplication;

public class TestTicketingApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(TicketingApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
