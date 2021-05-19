package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.agent.Agent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.io.IOException;

@SpringBootApplication
public class SpringBootApp extends SpringBootServletInitializer {

    public static void main(String[] args) throws IOException {
        AttachInFirstLineOfMain.attach();
        SpringApplication.run(SpringBootApp.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
        return applicationBuilder.sources(SpringBootApp.class);
    }
}
