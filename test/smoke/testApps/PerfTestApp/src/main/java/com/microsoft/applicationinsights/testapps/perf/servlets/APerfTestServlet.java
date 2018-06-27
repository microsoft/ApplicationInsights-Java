package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class APerfTestServlet extends HttpServlet {
  @Override
  protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    Stopwatch sw = Stopwatch.createStarted();
    try {
      reallyDoGet(req, resp);
    } finally {
      sw.stop();
      if (!resp.isCommitted()) { //  this should catch if something already responded
        final PrintWriter writer = resp.getWriter();
        writer.println(sw.elapsed(TimeUnit.MILLISECONDS));
        writer.flush();
        resp.setStatus(200);
      }
    }
  }

  protected abstract void reallyDoGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException;
}
