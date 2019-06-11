package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Servlet implementation class SimpleTestTraceLog4j2Servlet
 */
@WebServlet(description = "calls log4j2", urlPatterns = "/traceLog4j2")
public class SimpleTestTraceLog4j2Servlet extends HttpServlet {

	private static final long serialVersionUID = 3048578125004678364L;

	private static final Logger logger = LogManager.getRootLogger();

	/**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);
        logger.trace("This is log4j2 trace.");
        logger.debug("This is log4j2 debug.");
        logger.info("This is log4j2 info.");
        logger.warn("This is log4j2 warn.");
        logger.error("This is log4j2 error.");
        logger.fatal("This is log4j2 fatal.");
    }
}