// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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
public class MongoServlet extends HttpServlet {

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
