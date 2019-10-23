package com.microsoft.ajl.simple;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {

    @KafkaListener(topics = "mytopic", groupId = "mygroup")
    public void message(String message) {
        System.out.println("received: " + message);
    }
}
