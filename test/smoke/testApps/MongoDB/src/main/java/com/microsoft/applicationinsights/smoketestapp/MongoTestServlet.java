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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.util.concurrent.Callable;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;

@WebServlet("/*")
public class MongoTestServlet extends HttpServlet {

  public void init() throws ServletException {
    try {
      setupMongo();
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
    if (pathInfo.equals("/mongo")) {
      mongo();
    } else if (!pathInfo.equals("/")) {
      throw new ServletException("Unexpected url: " + pathInfo);
    }
  }

  private void mongo() throws Exception {
    MongoClient mongoClient = getMongoClient();
    executeFind(mongoClient);
    mongoClient.close();
  }

  private static void executeFind(MongoClient mongoClient) {
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("test");
    MongoCursor<Document> i = collection.find().iterator();
    while (i.hasNext()) {
      i.next();
    }
  }

  private static void setupMongo() throws Exception {
    MongoClient mongoClient = getMongoClient();
    setup(mongoClient);
    mongoClient.close();
  }

  private static MongoClient getMongoClient() throws Exception {
    return getMongoClient(
        new Callable<MongoClient>() {
          @Override
          public MongoClient call() {
            String hostname = System.getenv("MONGO");
            return MongoClients.create("mongodb://" + hostname);
          }
        });
  }

  private static MongoClient getMongoClient(Callable<MongoClient> callable) throws Exception {
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

  private static void setup(MongoClient mongoClient) {
    MongoDatabase database = mongoClient.getDatabase("testdb");
    MongoCollection<Document> collection = database.getCollection("test");
    collection.insertOne(new Document("one", "two"));
  }
}
