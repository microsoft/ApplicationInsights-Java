// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JettyNativeHandlerApp {

  public static void main(String[] args) throws Exception {
    Server server = new Server(8080);
    server.setHandler(new SimpleHandlerEx());
    server.start();
    server.join();
  }

  public static class SimpleHandlerEx extends AbstractHandler {

    @Override
    public void handle(
        String target,
        Request baseRequest,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

      response.setContentType("text/plain;charset=utf-8");
      response.setStatus(HttpServletResponse.SC_OK);
      baseRequest.setHandled(true);
      response.getWriter().println("Hello there");
    }
  }
}
