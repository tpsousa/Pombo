package com.pombo;

import org.springframework.boot.SpringApplication;

public class TestPomboApplication {

	public static void main(String[] args) {
		SpringApplication.from(PomboApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
