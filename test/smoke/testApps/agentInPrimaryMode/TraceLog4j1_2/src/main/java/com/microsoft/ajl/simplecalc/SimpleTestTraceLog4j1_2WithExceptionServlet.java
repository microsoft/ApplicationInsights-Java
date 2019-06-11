package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Servlet implementation class SimpleTestTraceLog4j1_2
 */
@WebServlet(description = "calls log4j1.2", urlPatterns = "/traceLog4j1_2WithException")
public class SimpleTestTraceLog4j1_2WithExceptionServlet extends HttpServlet {

	private static final long serialVersionUID = 3307609352835364383L;

	/**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Logger logger = LogManager.getRootLogger();
        logger.error("This is an exception!", new Exception("Fake Exception")); 

    }
}