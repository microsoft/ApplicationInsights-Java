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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Callable;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class CassandraTestServlet extends HttpServlet {

  public void init() throws ServletException {
    try {
      setupCassandra();
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

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
    if (pathInfo.equals("/cassandra")) {
      cassandra();
    } else if (!pathInfo.equals("/")) {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
  }

  private void cassandra() throws Exception {
    Session session = getCassandraSession();
    executeFind(session);
    session.close();
  }

  private static void executeFind(Session session) {
    ResultSet results = session.execute("select * from test.test");
    for (Row row : results) {
      row.getInt("id");
    }
  }

  private static void setupCassandra() throws Exception {
    Session session = getCassandraSession();
    setup(session);
    session.close();
  }

  private static Session getCassandraSession() throws Exception {
    return getCassandraSession(
        new Callable<Session>() {
          @Override
          public Session call() {
            String hostname = System.getenv("CASSANDRA");
            Cluster cluster =
                Cluster.builder()
                    .addContactPointsWithPorts(Arrays.asList(new InetSocketAddress(hostname, 9042)))
                    .build();
            return cluster.connect();
          }
        });
  }

  private static Session getCassandraSession(Callable<Session> callable) throws Exception {
    Exception exception;
    long start = System.nanoTime();
    do {
      try {
        return callable.call();
      } catch (Exception e) {
        exception = e;
      }
    } while (NANOSECONDS.toSeconds(System.nanoTime() - start) < 30);
    throw exception;
  }

  private static void setup(Session session) {
    session.execute(
        "CREATE KEYSPACE IF NOT EXISTS test WITH REPLICATION ="
            + " { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }");
    session.execute("CREATE TABLE IF NOT EXISTS test.test (id int PRIMARY KEY, test text)");
    session.execute("TRUNCATE test.test");
    session.execute("INSERT INTO test.test (id, test) VALUES (1, 'test')");
  }
}
