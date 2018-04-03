package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SimpleTestRequestSlowWithResponseTime
 */
@WebServlet(description = "calls request slow", urlPatterns = "/requestSlow")
public class SimpleTestRequestSlowWithResponseTime extends HttpServlet {

    private static final long serialVersionUID = 3007663491446163538L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        try {
            TimeUnit.SECONDS.sleep(20);
        } catch (Exception ex) {
        }
    }
}