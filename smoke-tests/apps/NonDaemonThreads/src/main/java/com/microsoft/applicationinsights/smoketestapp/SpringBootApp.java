// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootApp {

  private static final Logger logger = LoggerFactory.getLogger("smoketestapp");

  public static void main(String[] args) throws IOException {
    if (args.length == 1 && args[0].equals("okhttp3")) {
      okHttp3();
      logger.info("done");
      return;
    }
    SpringApplication.run(SpringBootApp.class, args);
  }

  private static void okHttp3() throws IOException {
    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
    okhttp3.Request request =
        new okhttp3.Request.Builder().url("https://www.bing.com/search?q=test").build();
    okhttp3.Response response = client.newCall(request).execute();
    response.body().close();
    response.close();

    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
  }
}
