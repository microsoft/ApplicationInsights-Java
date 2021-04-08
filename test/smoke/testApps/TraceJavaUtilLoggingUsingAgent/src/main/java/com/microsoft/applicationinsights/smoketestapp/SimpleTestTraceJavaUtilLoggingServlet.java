package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "calls jul", urlPatterns = "/traceJavaUtilLogging")
public class SimpleTestTraceJavaUtilLoggingServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger("smoketestapp");

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ServletFuncs.geRrenderHtml(request, response);

        logger.finest("This is jul finest.");
        logger.finer("This is jul finer.");
        logger.fine("This is jul fine.");
        logger.config("This is jul config.");
        logger.info("This is jul info.");
        logger.warning("This is jul warning.");
        logger.severe("This is jul severe.");
    }
}
