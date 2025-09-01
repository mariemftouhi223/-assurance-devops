package com.mariem.assurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.scheduling.annotation.EnableAsync; // décommente si tu en as besoin

@SpringBootApplication
// @EnableAsync
public class AssuranceNetworkApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(AssuranceNetworkApiApplication.class, args);
	}
}
