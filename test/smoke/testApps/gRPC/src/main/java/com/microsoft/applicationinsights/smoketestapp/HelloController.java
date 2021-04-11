package com.microsoft.applicationinsights.smoketestapp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import com.google.common.base.Joiner;
import example.GreeterGrpc;
import example.GreeterGrpc.GreeterBlockingStub;
import example.GreeterGrpc.GreeterStub;
import example.Helloworld;
import example.Helloworld.Response;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping("/")
    public String root() {
        return "OK";
    }

    @RequestMapping("/simple")
    public String simple() {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 10203)
                .usePlaintext()
                .build();

        GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
        Helloworld.Response response = client.sayHello(Helloworld.Request.newBuilder()
                .setName("abc")
                .build());

        channel.shutdown();

        return "Sent and received: " + response.getMessage();
    }

    @RequestMapping("/conversation")
    public String conversation() throws InterruptedException {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 10203)
                .usePlaintext()
                .build();

        GreeterStub client = GreeterGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);
        List<String> responses = new CopyOnWriteArrayList<>();
        StreamObserver<Response> conversation = client.conversation(new StreamObserver<Response>() {
            @Override
            public void onNext(Response value) {
                responses.add(value.getMessage());
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });
        conversation.onNext(Helloworld.Response.newBuilder()
                .setMessage("wxyz")
                .build());

        latch.await();

        channel.shutdown();

        return "Sent and received: " + Joiner.on(", ").join(responses);
    }
}
