package com.example.changtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChangtestApplication {

	public static void main(String[] args) {
	    Line a = new Line();
	    String aaa = a.getLineCode();
	    System.out.println(aaa);

		SpringApplication.run(ChangtestApplication.class, args);
	}

}
