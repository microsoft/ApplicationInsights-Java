// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;

@WebServlet("/*")
public class JedisServlet extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      doGetInternal(req);
      resp.getWriter().println("ok");
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void doGetInternal(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    if (pathInfo.equals("/jedis")) {
      jedis();
    } else if (!pathInfo.equals("/")) {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
  }

  private void jedis() {
    String hostname = System.getenv("REDIS");
    Jedis jedis = new Jedis(hostname, 6379);
    jedis.get("test");
  }
}
