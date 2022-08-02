/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "calls request slow w/o Thread.sleep", urlPatterns = "/slowLoop")
public class SlowRequestCpuBoundServlet extends HttpServlet {

  private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(1_000);
  private static final ThreadLocalRandom rand = ThreadLocalRandom.current();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    long startTime = System.currentTimeMillis();

    ServletFuncs.geRrenderHtml(request, response);
    int responseTime = 25;
    String customResponseTime = request.getParameter("responseTime");
    if (customResponseTime != null) {
      try {
        responseTime = Integer.parseInt(customResponseTime);
        System.out.println("Custom responseTime = " + responseTime);
      } catch (NumberFormatException e) {
        System.err.printf("Invalid value for 'responseTime': '%s'%n", customResponseTime);
      }
    }

    long responseTimeMillis = responseTime * 1000L;
    BigDecimal sum = BigDecimal.ONE;
    int iterations = 0;
    for (; durationSince(startTime) < responseTimeMillis; iterations++) {
      BigDecimal operand = BigDecimal.valueOf(rand.nextDouble()).multiply(MAX_VALUE);
      sum = sum.add(operand);
    }
    // this is just to use the value so JIT doesn't remove any computation
    System.out.printf("Accumulated sum: %s (%d iterations)%n", sum.toString(), iterations);
  }

  private static long durationSince(long startTime) {
    return System.currentTimeMillis() - startTime;
  }
}
