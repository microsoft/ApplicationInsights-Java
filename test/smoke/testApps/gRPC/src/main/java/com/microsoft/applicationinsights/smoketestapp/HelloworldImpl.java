package com.microsoft.applicationinsights.smoketestapp;

import example.GreeterGrpc;
import example.Helloworld;
import example.Helloworld.Response;
import io.grpc.stub.StreamObserver;

public class HelloworldImpl extends GreeterGrpc.GreeterImplBase {

    public void sayHello(Helloworld.Request request, StreamObserver<Response> responseObserver) {

        responseObserver.onNext(Helloworld.Response.newBuilder()
                .setMessage("hi " + request.getName())
                .build());
        responseObserver.onCompleted();
    }

    public StreamObserver<Helloworld.Response> conversation(
            StreamObserver<Helloworld.Response> responseObserver) {

        return new StreamObserver<Helloworld.Response>() {
            @Override
            public void onNext(Helloworld.Response value) {
                responseObserver.onNext(Helloworld.Response.newBuilder()
                        .setMessage("one " + value.getMessage())
                        .build());
                responseObserver.onNext(Helloworld.Response.newBuilder()
                        .setMessage("two " + value.getMessage())
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        };
    }
}
