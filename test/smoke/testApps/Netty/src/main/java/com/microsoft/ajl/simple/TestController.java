package com.microsoft.ajl.simple;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }


    @GetMapping("/test")
    public Mono<String> test() {
        System.out.println("HI THIS IS A TEST!");
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                completableFuture.complete("hello");
            }
        });
        return Mono.fromFuture(completableFuture);
    }
}
