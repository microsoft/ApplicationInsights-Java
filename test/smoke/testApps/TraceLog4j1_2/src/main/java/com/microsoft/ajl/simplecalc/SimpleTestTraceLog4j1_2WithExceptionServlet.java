package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

@WebServlet(description = "calls log4j1.2", urlPatterns = "/traceLog4j1_2WithException")
public class SimpleTestTraceLog4j1_2WithExceptionServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Logger logger = LogManager.getLogger("test");
        MDC.put("MDC key", "MDC value");
        logger.error("This is an exception!", new Exception("Fake Exception"));
        MDC.remove("MDC key");
    }
}