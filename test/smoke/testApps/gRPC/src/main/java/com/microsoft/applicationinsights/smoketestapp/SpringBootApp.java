package com.microsoft.applicationinsights.smoketestapp;

import io.grpc.ServerBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootApp {

    public static void main(String[] args) throws Exception {

        ServerBuilder.forPort(10203)
                .addService(new HelloworldImpl())
                .build()
                .start();

        SpringApplication.run(SpringBootApp.class, args);
    }
}
