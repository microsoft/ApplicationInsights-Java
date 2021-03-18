package com.microsoft.ajl.simplecalc;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "calls request slow", urlPatterns = "/requestSlow")
public class SimpleTestRequestSlowWithResponseTime extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        int sleepTime = 25;
        final String customSleepTime = request.getParameter("sleeptime");
        if (StringUtils.isNotBlank(customSleepTime)) {
            try {
                sleepTime = Integer.parseInt(customSleepTime);
            } catch (NumberFormatException e) {
                System.err.printf("Invalid value for 'sleeptime': '%s'%n", customSleepTime);
            }
        }
        try {
            System.out.printf("Sleeping for %d seconds.%n", sleepTime);
            TimeUnit.SECONDS.sleep(sleepTime);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}