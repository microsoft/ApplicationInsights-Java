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
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.springframework.web.reactive.function.client.WebClient;

@WebServlet("/*")
public class HttpClientTestServlet extends HttpServlet {

    private final CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
    private final CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.createDefault();

    public HttpClientTestServlet() {
        httpAsyncClient.start();
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
        } else if (pathInfo.equals("/apacheHttpClient4")) {
            return apacheHttpClient4();
        } else if (pathInfo.equals("/apacheHttpClient4WithResponseHandler")) {
            return apacheHttpClient4WithResponseHandler();
        } else if (pathInfo.equals("/apacheHttpClient3")) {
            return apacheHttpClient3();
        } else if (pathInfo.equals("/apacheHttpAsyncClient")) {
            return apacheHttpAsyncClient();
        } else if (pathInfo.equals("/okHttp3")) {
            return okHttp3();
        } else if (pathInfo.equals("/okHttp2")) {
            return okHttp2();
        } else if (pathInfo.equals("/springWebClient")) {
            return springWebClient();
        } else if (pathInfo.equals("/httpURLConnection")) {
            return httpURLConnection();
        } else {
            throw new ServletException("Unexpected url: " + pathInfo);
        }
    }

    private int apacheHttpClient4() throws IOException {
        String url = "https://www.bing.com/search?q=spaces%20test";
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(get)) {
            return response.getStatusLine().getStatusCode();
        }
    }

    private int apacheHttpClient4WithResponseHandler() throws IOException {
        String url = "https://www.bing.com/search?q=spaces%20test";
        HttpGet get = new HttpGet(url);
        return httpClient.execute(get, new ResponseHandler<Integer>() {
            @Override
            public Integer handleResponse(HttpResponse response) {
                return response.getStatusLine().getStatusCode();
            }
        });
    }

    private int apacheHttpClient3() throws IOException {
        HttpClient httpClient3 = new org.apache.commons.httpclient.HttpClient();
        CookiePolicy.registerCookieSpec("PermitAllCookiesSpec", PermitAllCookiesSpec.class);
        httpClient3.getParams().setCookiePolicy("PermitAllCookiesSpec");
        String url = "https://www.bing.com/search?q=spaces%20test";
        GetMethod httpGet = new GetMethod(url);
        httpClient3.executeMethod(httpGet);
        httpGet.releaseConnection();
        return httpGet.getStatusCode();
    }

    private int apacheHttpAsyncClient() throws ExecutionException, InterruptedException, IOException {
        HttpGet get = new HttpGet("https://www.bing.com/search?q=spaces%20test");
        HttpResponse httpResponse = httpAsyncClient.execute(get, null).get();
        httpResponse.getEntity().getContent().close();
        return httpResponse.getStatusLine().getStatusCode();
    }

    private int okHttp3() throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://www.bing.com/search?q=spaces%20test")
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        response.body().close();
        return response.code();
    }

    private int okHttp2() throws IOException {
        com.squareup.okhttp.OkHttpClient client = new com.squareup.okhttp.OkHttpClient();
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url("https://www.bing.com/search?q=spaces%20test")
                .build();
        com.squareup.okhttp.Response response = client.newCall(request).execute();
        response.body().close();
        return response.code();
    }

    private int springWebClient() {
        return WebClient.create().get()
                .uri("https://www.bing.com/search?q=spaces%20test")
                .exchange()
                .map(response -> response.statusCode())
                .block().value();
    }

    private int httpURLConnection() throws IOException {
        URL obj = new URL("https://www.bing.com/search?q=spaces%20test");
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        // calling getContentType() first, since this triggered a bug previously in the instrumentation previously
        connection.getContentType();
        InputStream content = connection.getInputStream();
        // drain the content
        byte[] buffer = new byte[1024];
        int nRead;
        while ((nRead = content.read(buffer)) != -1) {
        }
        content.close();
        return connection.getResponseCode();
    }

    public static class PermitAllCookiesSpec extends CookieSpecBase {
        public void validate(String host, int port, String path, boolean secure, final Cookie cookie) {
        }
    }
}
