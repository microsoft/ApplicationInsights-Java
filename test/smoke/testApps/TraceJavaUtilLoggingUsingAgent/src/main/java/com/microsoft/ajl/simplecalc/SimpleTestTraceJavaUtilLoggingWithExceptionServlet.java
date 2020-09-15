package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SimpleTestTraceJavaUtilLoggingWithExceptionServlet
 */
@WebServlet(description = "calls jul with exception", urlPatterns = "/traceJavaUtilLoggingWithException")
public class SimpleTestTraceJavaUtilLoggingWithExceptionServlet extends HttpServlet {

    private static final long serialVersionUID = -4480938547356817795L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        java.util.logging.Logger logger = Logger.getLogger("root");
        logger.log(Level.SEVERE, "This is an exception!", new Exception("Fake Exception"));
    }
}
