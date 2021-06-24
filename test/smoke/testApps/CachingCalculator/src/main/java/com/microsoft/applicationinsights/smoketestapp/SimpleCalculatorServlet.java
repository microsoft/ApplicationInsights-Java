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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.smoketestapp.model.BinaryCalculation;
import com.microsoft.applicationinsights.smoketestapp.model.BinaryOperator;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;

@WebServlet(
    description = "Performs given calculation",
    urlPatterns = {"/doCalc"})
public class SimpleCalculatorServlet extends HttpServlet {

  private Jedis redis;

  public SimpleCalculatorServlet() {
    try {
      String redisHostname = System.getenv("REDIS");
      if (redisHostname != null) {
        redis = new Jedis(redisHostname, 6379);
      }
    } catch (Exception e) {
      System.err.println("Error with redis in servlet");
      e.printStackTrace();
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    BinaryCalculation bc;
    try {
      bc = readParameters(request.getParameterMap());
    } catch (CalculatorParameterException cpe) {
      String errMsg = cpe.getLocalizedMessage();
      System.err.println(errMsg);
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, errMsg);
      return;
    }

    if (bc == null) {
      System.out.println("No parameters given.");
      response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      return;
    }

    response.setContentType("text/html;charset=UTF-8");
    renderHtml(bc, response.getWriter());

    new TelemetryClient().trackMetric("TimeToRespond", 111222333);
  }

  private BinaryCalculation readParameters(Map<String, String[]> parameterMap)
      throws CalculatorParameterException {
    if (parameterMap == null) {
      throw new IllegalArgumentException("parameterMap cannot be null");
    }

    if (parameterMap.isEmpty()) {
      return null;
    }

    // log params
    System.out.println("Given parameters:");
    for (Entry<String, String[]> entry : parameterMap.entrySet()) {
      String pname = entry.getKey();
      System.out.printf("%s: %s%n", pname, Arrays.toString(entry.getValue()));
    }

    String strLopnd = parameterMap.get(ParameterConstants.LEFT_OPERAND)[0];
    String strRopnd = parameterMap.get(ParameterConstants.RIGHT_OPERAND)[0];
    String strOprtr = parameterMap.get(ParameterConstants.OPERATOR)[0];

    double lopnd = parseParamOrThrow(strLopnd, "Left operand is not a number: %s");
    double ropnd = parseParamOrThrow(strRopnd, "Right operand is not a number: %s");

    BinaryOperator op = BinaryOperator.fromVerb(strOprtr);
    if (op == null) {
      throw new CalculatorParameterException("Unknown operator: " + strOprtr);
    }

    if (redis != null) {
      try {
        redis.set("loperand", strLopnd);
        redis.set("operator", strOprtr);
        redis.set("roperand", strRopnd);
      } catch (Exception e) {
        System.err.println("Something went wrong trying to cache");
        e.printStackTrace();
      }
    }

    return new BinaryCalculation(lopnd, ropnd, op);
  }

  private static double parseParamOrThrow(String param, String errMsgFmt)
      throws CalculatorParameterException {
    try {
      return Double.parseDouble(param);
    } catch (NumberFormatException e) {
      throw new CalculatorParameterException(String.format(errMsgFmt, param), e);
    }
  }

  private static void renderHtml(BinaryCalculation calc, PrintWriter writer) {
    writer.println("<html>");
    writer.println("<head><title>Calculation Result</title></head>");
    writer.println("<body>");
    writer.printf(
        "<i>%s</i> %s <i>%s</i> = <b>%s</b>%n",
        calc.getLeftOperandFormatted(),
        calc.getOperatorSymbol(),
        calc.getRightOperandFormatted(),
        calc.resultFormatted());
    writer.println("<p><a href=\".\">Do Another Calculation</a></p>");
    writer.println("</body></html>");
  }
}
