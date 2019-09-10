package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class SimpleTestTraceJavaUtilLoggingServlet
 */
@WebServlet(description = "calls jul", urlPatterns = "/traceJavaUtilLogging")
public class SimpleTestTraceJavaUtilLoggingServlet extends HttpServlet {

    private static final long serialVersionUID = 8803657641175323998L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Logger logger = Logger.getLogger("root");
        logger.finest("This is jul finest.");
        logger.finer("This is jul finer.");
        logger.fine("This is jul fine.");
        logger.config("This is jul config.");
        logger.info("This is jul info.");
        logger.warning("This is jul warning.");
        logger.severe("This is jul severe.");
    }
}
