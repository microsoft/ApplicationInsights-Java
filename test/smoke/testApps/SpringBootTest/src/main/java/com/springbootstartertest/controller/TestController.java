package com.springbootstartertest.controller;

import com.microsoft.applicationinsights.TelemetryClient;
import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {

	private static final TelemetryClient client = new TelemetryClient();

	private CloseableHttpClient httpClient = HttpClientBuilder.create().disableAutomaticRetries().build();
	
	@GetMapping("/")
	public String rootPage() {
		return "OK";
	}

	@GetMapping("/basic/trackEvent")
	public String trackEventSpringBoot() {
		Map<String, String> properties = new HashMap<String, String>() {
			{
				put("key", "value");
			}
		};
		Map<String, Double> metrics = new HashMap<String, Double>() {
			{
				put("key", 1d);
			}
		};

		//Event
		client.trackEvent("EventDataTest");
		client.trackEvent("EventDataPropertyTest", properties, metrics);
		return "hello";
	}

	@GetMapping("/throwsException")
	public void resultCodeTest() throws Exception {
		throw new ServletException("This is an exception");
	}

    @GetMapping("/asyncDependencyCallWithApacheHttpClient4")
    public AsyncResult<Integer> asyncDependencyCallWithApacheHttpClient4() throws IOException {
        String url = "https://www.bing.com";
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(get)){
            return new AsyncResult<>(response.getStatusLine().getStatusCode());
        }
    }

    @GetMapping("/asyncDependencyCallWithApacheHttpClient3")
    public AsyncResult<Integer> asyncDependencyCallWithApacheHttpClient3() throws IOException {
        HttpClient httpClient3 = new org.apache.commons.httpclient.HttpClient();
        CookiePolicy.registerCookieSpec("PermitAllCookiesSpec", PermitAllCookiesSpec.class);
        httpClient3.getParams().setCookiePolicy("PermitAllCookiesSpec");
        String url = "https://www.bing.com";
        GetMethod httpGet = new GetMethod(url);
        httpClient3.executeMethod(httpGet);
        httpGet.releaseConnection();
        return new AsyncResult<>(httpGet.getStatusCode());
    }

    @GetMapping("/asyncDependencyCallWithOkHttp3")
    public AsyncResult<Integer> asyncDependencyCallWithOkHttp3() throws IOException {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url("https://www.bing.com")
                .build();
        okhttp3.Response response = client.newCall(request).execute();
        response.body().close();
        return new AsyncResult<>(response.code());
    }

    @GetMapping("/asyncDependencyCallWithOkHttp2")
    public AsyncResult<Integer> asyncDependencyCallWithOkHttp2() throws IOException {
        com.squareup.okhttp.OkHttpClient client = new com.squareup.okhttp.OkHttpClient();
        com.squareup.okhttp.Request request = new com.squareup.okhttp.Request.Builder()
                .url("https://www.bing.com")
                .build();
        com.squareup.okhttp.Response response = client.newCall(request).execute();
        response.body().close();
        return new AsyncResult<>(response.code());
    }

    public static class PermitAllCookiesSpec extends CookieSpecBase {
        public void validate(String host, int port, String path, boolean secure, final Cookie cookie)
                throws MalformedCookieException {
        }
    }
}
