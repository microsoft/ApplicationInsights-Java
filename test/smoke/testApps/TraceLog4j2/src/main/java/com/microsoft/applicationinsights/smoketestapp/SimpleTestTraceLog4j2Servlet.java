package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

@WebServlet(description = "calls log4j2", urlPatterns = "/traceLog4j2")
public class SimpleTestTraceLog4j2Servlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger("smoketestapp");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);
        logger.trace("This is log4j2 trace.");
        logger.debug("This is log4j2 debug.");
        logger.info("This is log4j2 info.");
        ThreadContext.put("MDC key", "MDC value");
        logger.warn("This is log4j2 warn.");
        ThreadContext.remove("MDC key");
        logger.error("This is log4j2 error.");
        logger.fatal("This is log4j2 fatal.");
    }
}