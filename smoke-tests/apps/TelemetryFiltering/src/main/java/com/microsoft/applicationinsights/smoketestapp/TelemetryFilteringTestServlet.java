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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Callable;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hsqldb.jdbc.JDBCDriver;

@WebServlet("/*")
public class TelemetryFilteringTestServlet extends HttpServlet {

  public void init() throws ServletException {
    try {
      setupHsqldb();
    } catch (Exception e) {
      // surprisingly not all application servers seem to log init exceptions to stdout
      e.printStackTrace();
      throw new ServletException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      int statusCode = doGetInternal(req);
      resp.getWriter().println(statusCode);
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private int doGetInternal(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    if (pathInfo.equals("/")) {
      return 200;
    } else if (pathInfo.equals("/health-check")) {
      Connection connection = getHsqldbConnection();
      executeStatement(connection);
      connection.close();
      return 200;
    } else if (pathInfo.equals("/login")) {
      Connection connection = getHsqldbConnection();
      executeStatement(connection);
      connection.close();
      return 200;
    } else if (pathInfo.equals("/noisy-jdbc")) {
      Connection connection = getHsqldbConnection();
      executeNoisyStatement(connection);
      connection.close();
      return 200;
    } else if (pathInfo.equals("/regular-jdbc")) {
      Connection connection = getHsqldbConnection();
      executeStatement(connection);
      connection.close();
      return 200;
    } else {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
  }

  private static Connection getHsqldbConnection() throws Exception {
    return JDBCDriver.getConnection("jdbc:hsqldb:mem:testdb", null);
  }

  private void executeStatement(Connection connection) throws SQLException {
    Statement statement = connection.createStatement();
    ResultSet rs = statement.executeQuery("select * from abc");
    while (rs.next()) {}
    rs.close();
    statement.close();
  }

  private void executeNoisyStatement(Connection connection) throws SQLException {
    Statement statement = connection.createStatement();
    ResultSet rs = statement.executeQuery("select count(*) from abc");
    while (rs.next()) {}
    rs.close();
    statement.close();
  }

  private static void setupHsqldb() throws Exception {
    Connection connection =
        getConnection(
            new Callable<Connection>() {
              @Override
              public Connection call() throws Exception {
                return getHsqldbConnection();
              }
            });
    setup(connection);
    connection.close();
  }

  private static Connection getConnection(Callable<Connection> callable) throws Exception {
    Exception exception;
    long startTimeMillis = System.currentTimeMillis();
    do {
      try {
        return callable.call();
      } catch (Exception e) {
        exception = e;
      }
    } while (System.currentTimeMillis() - startTimeMillis < 30000);
    throw exception;
  }

  private static void setup(Connection connection) throws SQLException {
    Statement statement = connection.createStatement();
    try {
      statement.execute("create table abc (xyz varchar(10))");
      statement.execute("insert into abc (xyz) values ('x')");
      statement.execute("insert into abc (xyz) values ('y')");
      statement.execute("insert into abc (xyz) values ('z')");
    } finally {
      statement.close();
    }
  }
}
