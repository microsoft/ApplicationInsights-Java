package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class CustomInstrumentationController extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      String response = doGetInternal(req);
      resp.getWriter().println(response);
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private String doGetInternal(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    if (pathInfo.equals("/")) {
      return "ok";
    } else if (pathInfo.equals("/customInstrumentationOne")) {
      return customInstrumentationOne();
    } else if (pathInfo.equals("/customInstrumentationTwo")) {
      return customInstrumentationTwo();
    } else if (pathInfo.equals("/customInstrumentationThree")) {
      return customInstrumentationThree();
    } else if (pathInfo.equals("/customInstrumentationFour")) {
      return customInstrumentationFour();
    } else if (pathInfo.equals("/customInstrumentationFive")) {
      return customInstrumentationFive();
    } else if (pathInfo.equals("/customInstrumentationSeven")) {
      return customInstrumentationSeven();
    } else if (pathInfo.equals("/customInstrumentationEight")) {
      return customInstrumentationEight();
    } else if (pathInfo.equals("/customInstrumentationNine")) {
      return customInstrumentationNine();
    } else {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
  }

  public String customInstrumentationOne() {
    return new TargetObject().one();
  }

  public String customInstrumentationTwo() {
    return new TargetObject().two("Two");
  }

  public String customInstrumentationThree() {
    try {
      return new TargetObject().three();
    } catch (Exception e) {
      return "Three";
    }
  }

  public String customInstrumentationFour() {
    new TargetObject.NestedObject().four(false, null, null);
    return "Four";
  }

  public String customInstrumentationFive() {
    return new TargetObject().five();
  }

  public String customInstrumentationSeven() {
    return new TargetObject().seven("Seven");
  }

  public String customInstrumentationEight() {
    return new TargetObject().eight("Eight");
  }

  public String customInstrumentationNine() throws IOException {
    return new TargetObject().nine();
  }
}
