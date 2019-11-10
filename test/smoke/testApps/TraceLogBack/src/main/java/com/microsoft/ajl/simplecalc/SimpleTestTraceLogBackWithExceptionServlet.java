package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet implementation class SimpleTestTraceLogBackWithExceptionServlet
 */
@WebServlet(description = "calls logback with exception", urlPatterns = "/traceLogBackWithException")
public class SimpleTestTraceLogBackWithExceptionServlet extends HttpServlet {

    private static final long serialVersionUID = -4480938547356817795L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Logger logger = LoggerFactory.getLogger("root");
        logger.error("This is an exception!", new Exception("Fake Exception"));
    }
}