// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.springframework.web.reactive.function.client.WebClient;

@WebServlet("/*")
public class HttpClientServlet extends HttpServlet {

  private final CloseableHttpClient httpClient =
      HttpClientBuilder.create().disableAutomaticRetries().build();
  private final CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();

  public HttpClientServlet() {
    httpAsyncClient.start();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
    try {
      doGetInternal(req);
      resp.getWriter().println("hi!");
    } catch (ServletException e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void doGetInternal(HttpServletRequest req) throws Exception {
    String pathInfo = req.getPathInfo();
    final ExecuteGetUrl executeGetUrl;
    switch (pathInfo) {
      case "/":
        executeGetUrl = null;
        break;
      case "/apacheHttpClient4":
        executeGetUrl = this::apacheHttpClient4;
        break;
      case "/apacheHttpClient4WithResponseHandler":
        executeGetUrl = this::apacheHttpClient4WithResponseHandler;
        break;
      case "/apacheHttpClient3":
        executeGetUrl = this::apacheHttpClient3;
        break;
      case "/apacheHttpAsyncClient":
        executeGetUrl = this::apacheHttpAsyncClient;
        break;
      case "/okHttp3":
        executeGetUrl = this::okHttp3;
        break;
      case "/okHttp2":
        executeGetUrl = this::okHttp2;
        break;
      case "/springWebClient":
        executeGetUrl = this::springWebClient;
        break;
      case "/httpUrlConnection":
        executeGetUrl = this::httpUrlConnection;
        break;
      default:
        throw new ServletException("Unexpected url: " + pathInfo);
    }

    if (executeGetUrl != null) {
      executeGetUrl.execute("https://mock.codes/200?q=spaces%20test");
      try {
        executeGetUrl.execute("https://mock.codes/404");
      } catch (Exception e) {
        // HttpURLConnection throws exception on 404 and 500
      }
      try {
        executeGetUrl.execute("https://mock.codes/500");
      } catch (Exception e) {
        // HttpURLConnection throws exception on 404 and 500
      }
    }
  }

  private void apacheHttpClient4(String url) throws IOException {
    HttpGet get = new HttpGet(url);
    httpClient.execute(get).close();
  }

  private void apacheHttpClient4WithResponseHandler(String url) throws IOException {
    HttpGet get = new HttpGet(url);
    httpClient.execute(get, response -> response.getStatusLine().getStatusCode());
  }

  private void apacheHttpClient3(String url) throws IOException {
    HttpClient httpClient3 = new org.apache.commons.httpclient.HttpClient();
    CookiePolicy.registerCookieSpec("PermitAllCookiesSpec", PermitAllCookiesSpec.class);
    httpClient3.getParams().setCookiePolicy("PermitAllCookiesSpec");
    GetMethod httpGet = new GetMethod(url);
    httpClient3.executeMethod(httpGet);
    httpGet.releaseConnection();
  }

  private void apacheHttpAsyncClient(String url)
      throws ExecutionException, InterruptedException, IOException {
    HttpGet get = new HttpGet(url);
    HttpResponse httpResponse = httpAsyncClient.execute(get, null).get();
    httpResponse.getEntity().getContent().close();
  }

  private void okHttp3(String url) throws IOException {
    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
    okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
    okhttp3.Response response = client.newCall(request).execute();
    response.body().close();
    response.code();
  }

  private void okHttp2(String url) throws IOException {
    com.squareup.okhttp.OkHttpClient client = new com.squareup.okhttp.OkHttpClient();
    com.squareup.okhttp.Request request =
        new com.squareup.okhttp.Request.Builder().url(url).build();
    client.newCall(request).execute().body().close();
  }

  private void springWebClient(String url) {
    WebClient.create().get().uri(url).exchange().map(response -> response.statusCode()).block();
  }

  private void httpUrlConnection(String url) throws IOException {
    URL obj = new URL(url);
    HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
    // calling getContentType() first, since this triggered a bug previously in the instrumentation
    // previously
    connection.getContentType();
    InputStream content = connection.getInputStream();
    // drain the content
    byte[] buffer = new byte[1024];
    while (content.read(buffer) != -1) {}
    content.close();
  }

  @FunctionalInterface
  interface ExecuteGetUrl {
    void execute(String url) throws Exception;
  }

  public static class PermitAllCookiesSpec extends CookieSpecBase {
    @Override
    public void validate(String host, int port, String path, boolean secure, Cookie cookie) {}
  }
}
