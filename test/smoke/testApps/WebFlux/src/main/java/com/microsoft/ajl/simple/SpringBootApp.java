package com.microsoft.ajl.simple;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SpringBootApp {

    public static void main(String[] args) {

        SpringApplication.run(SpringBootApp.class, args);
    }
}
