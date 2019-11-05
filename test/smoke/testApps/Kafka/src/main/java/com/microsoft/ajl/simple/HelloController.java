package com.microsoft.ajl.simple;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @RequestMapping("/")
    public String root() {
        return "OK";
    }

    @RequestMapping("/sendMessage")
    public String sendMessage() throws ExecutionException, InterruptedException {
        kafkaTemplate.send("mytopic", "hello world!").get();
        return "Sent!";
    }
}
