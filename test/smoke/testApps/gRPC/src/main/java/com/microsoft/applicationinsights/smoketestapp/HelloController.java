/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketestapp;

import example.GreeterGrpc;
import example.GreeterGrpc.GreeterBlockingStub;
import example.GreeterGrpc.GreeterStub;
import example.Helloworld;
import example.Helloworld.Response;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
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

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 10203).usePlaintext().build();

    GreeterBlockingStub client = GreeterGrpc.newBlockingStub(channel);
    Helloworld.Response response =
        client.sayHello(Helloworld.Request.newBuilder().setName("abc").build());

    channel.shutdown();

    return "Sent and received: " + response.getMessage();
  }

  @RequestMapping("/conversation")
  public String conversation() throws InterruptedException {

    ManagedChannel channel =
        ManagedChannelBuilder.forAddress("localhost", 10203).usePlaintext().build();

    GreeterStub client = GreeterGrpc.newStub(channel);
    CountDownLatch latch = new CountDownLatch(1);
    List<String> responses = new CopyOnWriteArrayList<>();
    StreamObserver<Response> conversation =
        client.conversation(
            new StreamObserver<Response>() {
              @Override
              public void onNext(Response value) {
                responses.add(value.getMessage());
              }

              @Override
              public void onError(Throwable t) {}

              @Override
              public void onCompleted() {
                latch.countDown();
              }
            });
    conversation.onNext(Helloworld.Response.newBuilder().setMessage("wxyz").build());

    latch.await();

    channel.shutdown();

    return "Sent and received: " + String.join(", ", responses);
  }
}
