package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

@WebServlet(description = "calls log4j2 with exception", urlPatterns = "/traceLog4j2WithException")
public class SimpleTestTraceLog4j2WithExceptionServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger("test");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        ThreadContext.put("MDC key", "MDC value");
        logger.error("This is an exception!", new Exception("Fake Exception"));
        ThreadContext.remove("MDC key");
    }
}