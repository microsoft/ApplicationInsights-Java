package com.microsoft.applicationinsights.smoketestapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class SpringBootApp {

    public static void main(String[] args) throws IOException {
        if (args.length == 1 && args[0].equals("okhttp3")) {
            okHttp3();
            return;
        }
        SpringApplication.run(SpringBootApp.class, args);
    }

    private static void okHttp3() throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://www.bing.com/search?q=test")
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        response.body().close();
        response.close();

        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }
}
