package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Servlet implementation class SimpleTestTraceLog4j2WithExceptionServlet */
@WebServlet(description = "calls log4j2 with exception", urlPatterns = "/traceLog4j2WithException")
public class SimpleTestTraceLog4j2WithExceptionServlet extends HttpServlet {

  private static final long serialVersionUID = 9101440811626233466L;

  /** @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response) */
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    ServletFuncs.geRrenderHtml(request, response);

    Logger logger = LogManager.getRootLogger();
    logger.error("This is an exception!", new Exception("Fake Exception"));
  }
}
