package com.microsoft.applicationinsights.smoketest;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class HttpHelper {

	public static String get(String url) throws UnsupportedOperationException, IOException {
		CloseableHttpClient client = getHttpClient();
		try {
			HttpGet get = new HttpGet(url);
			CloseableHttpResponse resp1 = client.execute(get);
			return extractResponseBody(resp1);
		}
		finally {
			client.close();
		}
	}

	private static CloseableHttpClient getHttpClient() {
		return HttpClientBuilder.create()
				.disableAutomaticRetries()
				.build();
	}

	public static String post(String url, String body) throws ClientProtocolException, IOException {
		CloseableHttpClient client = getHttpClient();
		try {
			HttpPost post = new HttpPost(url);
			post.setEntity(new StringEntity("PING"));
			CloseableHttpResponse resp1 = client.execute(post);
			return extractResponseBody(resp1);
		}
		finally {
			client.close();
		}
	}

	private static String extractResponseBody(CloseableHttpResponse resp) throws IOException {
		try {
            HttpEntity entity = resp.getEntity();
            StringWriter cw = new StringWriter();
            CharStreams.copy(new InputStreamReader(entity.getContent()), cw);
            EntityUtils.consume(entity);
            return cw.toString();
        }
        finally {
            resp.close();
        }
	}
}