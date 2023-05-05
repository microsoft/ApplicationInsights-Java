// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootApp {
  public static void main(String[] args) {
    System.setProperty("applicationinsights.debug.retainJfrFile", "true");
    SpringApplication.run(SpringBootApp.class, args);
  }
}
