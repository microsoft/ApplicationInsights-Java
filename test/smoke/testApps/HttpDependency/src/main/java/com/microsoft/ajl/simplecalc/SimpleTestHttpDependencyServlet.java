package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * Servlet implementation class SimpleTestHttpDependencyServlet
 */
@WebServlet(description = "calls http dependency", urlPatterns = "/httpDependency")
public class SimpleTestHttpDependencyServlet extends HttpServlet {

    private static final long serialVersionUID = 387965757798573509L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        // Get true http use apache http client
        getHttpUseApacheHttpClient("https://www.bing.com");

        // Get true http use ok http client
        getHttpUseOkHttpClient("https://www.microsoft.com");

        // Get false http use ok http client
        try {
            getHttpUseOkHttpClient("https://www.microsoftabc.com");
        } catch (Exception e) {
            // TODO: handle exception
        }

        // Get false http use apache http client
        getHttpUseApacheHttpClient("https://www.bingxxxxx.com");
    }

    private void getHttpUseApacheHttpClient(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        HttpResponse httpResponse = client.execute(get);
    }

    private void getHttpUseOkHttpClient(String url) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
    }
}