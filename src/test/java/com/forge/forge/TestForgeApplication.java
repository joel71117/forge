package com.forge.forge;

import org.springframework.boot.SpringApplication;

public class TestForgeApplication {

	public static void main(String[] args) {
		SpringApplication.from(ForgeApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
