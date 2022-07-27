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
import com.microsoft.applicationinsights.telemetry.ExceptionTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
    description = "Performs given calculation",
    urlPatterns = {"/trackException"})
public class SimpleTrackExceptionServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    ServletFuncs.geRrenderHtml(request, response);

    Exception exception = new Exception("This is track exception.");

    Map<String, String> properties =
        new HashMap<String, String>() {
          {
            put("key", "value");
          }
        };
    Map<String, Double> metrics =
        new HashMap<String, Double>() {
          {
            put("key", 1d);
          }
        };

    client.trackException(exception);
    client.trackException(exception, properties, metrics);

    ExceptionTelemetry et = new ExceptionTelemetry(exception);
    et.setSeverityLevel(SeverityLevel.Error);
    client.track(et);
  }
}
