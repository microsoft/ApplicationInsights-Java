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

package com.microsoft.applicationinsights.web.utils;

import com.microsoft.applicationinsights.web.internal.WebRequestTrackingFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by yonisha on 2/3/2015.
 */
public class JettyTestServer {
    private Server server;
    private final int Min = 1050;
    private final int Max = 15000;
    private final int portNumber;

    public JettyTestServer() {
        // try 256 times for ports
        int initialPortNumber = Min + (int)(Math.random() * ((Max - Min) + 1));
        for (int i = 0; i < 256; i++) {
            ServerSocket ss = null;
            try {
                ss = new ServerSocket(initialPortNumber);
                break; // if it doesn't throw, the port is open
            } catch (IOException e) {
                System.out.printf("port '%d' in use. Trying next one.%n", initialPortNumber);
                initialPortNumber++;
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        System.err.printf("Error closing port testing socket:%n%s%n", e.toString());
                    }
                }
            }
        }
        portNumber = initialPortNumber;
    }

    public void start() throws Exception {
        server = new Server(portNumber);

        //Initialize the server
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        context.addServlet(TestServlet.class, "/");
        context.addFilter(WebRequestTrackingFilter.class, "/*", FilterMapping.ALL);

        server.setHandler(context);
        server.start();
    }

    public void shutdown() throws Exception {
        if (server == null) {
            return;
        }

        server.stop();
        server.destroy();
    }

    public int getPortNumber() {
        return portNumber;
    }
}
