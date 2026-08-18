package com.forge;

import org.springframework.boot.SpringApplication;

public class TestForgeApplication {

	public static void main(String[] args) {
		SpringApplication.from(ForgeApplication::main).run(args);
	}

}
