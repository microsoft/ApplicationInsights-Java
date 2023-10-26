package com.microsoft.applicationinsights.smoketest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OtlpApplication {

  public OtlpApplication() {}
  public static void main(String[] args) {
    SpringApplication.run(OtlpApplication.class, args);
  }
}

