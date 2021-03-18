package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@WebServlet(urlPatterns = "/traceLogBackWithException")
public class SimpleTestTraceLogBackWithExceptionServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Logger logger = LoggerFactory.getLogger("test");
        MDC.put("MDC key", "MDC value");
        logger.error("This is an exception!", new Exception("Fake Exception"));
        MDC.remove("MDC key");
    }
}