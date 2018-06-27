package com.microsoft.ajl.simplecalc;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import java.io.IOException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet implementation class SimpleTrackHttpRequestServlet */
@WebServlet(
  description = "Performs given calculation",
  urlPatterns = {"/trackHttpRequest"}
)
public class SimpleTrackHttpRequestServlet extends HttpServlet {

  private static final long serialVersionUID = -1484210841610659769L;
  private TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    ServletFuncs.geRrenderHtml(request, response);

    // true
    client.trackHttpRequest("HttpRequestDataTest", new Date(), 4711, "200", true);

    RequestTelemetry rt = new RequestTelemetry("PingTest", new Date(), 1, "200", true);
    rt.setUrl("http://tempuri.org/ping");
    client.track(rt);

    // false
    client.trackHttpRequest("FailedHttpRequest", new Date(), 6666, "404", false);

    RequestTelemetry rt2 =
        new RequestTelemetry("FailedHttpRequest2", new Date(), 8888, "505", false);
    rt2.setUrl("https://www.bingasdasdasdasda.com/");
    client.track(rt2);
  }
}
