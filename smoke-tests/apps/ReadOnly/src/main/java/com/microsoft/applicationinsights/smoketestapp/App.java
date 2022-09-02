// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.util.logging.Logger;

// Spring boot doesn't support read-only containers
// (https://github.com/spring-projects/spring-boot/issues/8578)
public class App {

  private static final Logger logger = Logger.getLogger("smoketestapp");

  public static void main(String[] args) throws IOException {
    logger.info("hello");
  }
}
