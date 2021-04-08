package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

@WebServlet(description = "calls log4j1.2", urlPatterns = "/traceLog4j1_2")
public class SimpleTestTraceLog4j1_2Servlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger("test");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        logger.trace("This is log4j1.2 trace.");
        logger.debug("This is log4j1.2 debug.");
        logger.info("This is log4j1.2 info.");
        MDC.put("MDC key", "MDC value");
        logger.warn("This is log4j1.2 warn.");
        MDC.remove("MDC key");
        logger.error("This is log4j1.2 error.");
        logger.fatal("This is log4j1.2 fatal.");
    }
}