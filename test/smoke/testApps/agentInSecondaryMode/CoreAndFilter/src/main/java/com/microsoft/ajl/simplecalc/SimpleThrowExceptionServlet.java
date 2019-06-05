package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet Servlet implementation class SimpleThrowExceptionServlet
 */
@WebServlet(description = "throw an servlet execption", urlPatterns = { "/autoExceptionWithFailedRequest" })
public class SimpleThrowExceptionServlet extends HttpServlet {

    private static final long serialVersionUID = 5079705745146112889L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

		throw new ServletException("This is a auto thrown exception !");      
    }

}