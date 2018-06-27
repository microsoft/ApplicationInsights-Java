package com.microsoft.ajl.simplecalc;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet implementation class SimpleTrackTraceServlet */
@WebServlet(
  description = "calls trackPageView twice; once vanilla, once with properties",
  urlPatterns = {"/trackPageView"}
)
public class SimpleTrackPageViewServlet extends HttpServlet {
  private static final long serialVersionUID = -633683109556605395L;
  private TelemetryClient client = new TelemetryClient();

  /** @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response) */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    client.trackPageView("test-page");

    PageViewTelemetry pvt = new PageViewTelemetry("test-page-2");
    pvt.getProperties().put("key", "value");
    pvt.setDuration(123456);
    client.trackPageView(pvt);

    ServletFuncs.geRrenderHtml(request, response);
  }
}
