package com.microsoft.ajl.simplecalc;

import com.microsoft.applicationinsights.TelemetryClient;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Servlet implementation class SimpleTrackMetricServlet */
@WebServlet(
  description = "Performs given calculation",
  urlPatterns = {"/trackMetric"}
)
public class SimpleTrackMetricServlet extends HttpServlet {

  private static final long serialVersionUID = -7579571044049925445L;
  private TelemetryClient client = new TelemetryClient();

  /** @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response) */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    ServletFuncs.geRrenderHtml(request, response);

    client.trackMetric("TimeToRespond", 111222333);
  }
}
