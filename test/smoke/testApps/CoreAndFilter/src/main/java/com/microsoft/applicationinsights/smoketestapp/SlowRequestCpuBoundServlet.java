package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@WebServlet(description = "calls request slow w/o Thread.sleep", urlPatterns = "/slowLoop")
public class SlowRequestCpuBoundServlet extends HttpServlet {

    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(1_000);
    private static final ThreadLocalRandom rand = ThreadLocalRandom.current();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final long startTime = System.currentTimeMillis();

        ServletFuncs.geRrenderHtml(request, response);
        int responseTime = 25;
        final String customRepsonseTime = request.getParameter("responseTime");
        if (customRepsonseTime != null) {
            try {
                responseTime = Integer.parseInt(customRepsonseTime);
                System.out.println("Custom responseTime = "+responseTime);
            } catch (NumberFormatException e) {
                System.err.printf("Invalid value for 'responseTime': '%s'%n", customRepsonseTime);
            }
        }

        final long responseTimeMillis = responseTime * 1000L;
        BigDecimal sum = BigDecimal.ONE;
        int iterations = 0;
        for (; durationSince(startTime) < responseTimeMillis; iterations++) {
                final BigDecimal operand = BigDecimal.valueOf(rand.nextDouble()).multiply(MAX_VALUE);
                sum = sum.add(operand);
        }
        // this is just to use the value so JIT doesn't remove any computation
        System.out.printf("Accumulated sum: %s (%d iterations)%n", sum.toString(), iterations);
    }

    private static long durationSince(long startTime) {
        return System.currentTimeMillis() - startTime;
    }
}