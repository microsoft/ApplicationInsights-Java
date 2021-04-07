package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "calls jul with exception", urlPatterns = "/traceJavaUtilLoggingWithException")
public class SimpleTestTraceJavaUtilLoggingWithExceptionServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        java.util.logging.Logger logger = Logger.getLogger("root");
        logger.log(Level.SEVERE, "This is an exception!", new Exception("Fake Exception"));
    }
}
