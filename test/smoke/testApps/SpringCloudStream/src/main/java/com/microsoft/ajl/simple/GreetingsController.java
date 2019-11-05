package com.microsoft.ajl.simple;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingsController {

    private final GreetingsService greetingsService;

    public GreetingsController(GreetingsService greetingsService) {
        this.greetingsService = greetingsService;
    }

    @RequestMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/sendMessage")
    public String sendMessage() {
        Greetings greetings = new Greetings(System.currentTimeMillis(), "hello world!");
        greetingsService.sendGreeting(greetings);
        return "Sent!";
    }
}
