package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.google.common.io.CharStreams;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

@WebServlet({"/fakeRest"})
public class FakeRestServlet extends APerfTestServlet {
  @Override
  protected void reallyDoGet(final HttpServletRequest req, HttpServletResponse resp) {
    new TestCaseRunnable(
            new Runnable() {
              @Override
              public void run() {
                TelemetryClient tc = new TelemetryClient();
                final String pUrl = req.getParameter("url");
                final String url =
                    (pUrl == null)
                        ? "http://www.msn.com"
                        : ((pUrl.startsWith("http://")) ? pUrl : "http://" + pUrl);

                CloseableHttpClient client = HttpClients.createDefault();
                try {
                  HttpGet get = new HttpGet(url);
                  CloseableHttpResponse getResp = client.execute(get);
                  try {
                    HttpEntity entity = getResp.getEntity();
                    StringWriter cw = new StringWriter();
                    CharStreams.copy(new InputStreamReader(entity.getContent()), cw);
                    EntityUtils.consume(entity);
                    StatusLine status = getResp.getStatusLine();
                    System.out.printf(
                        "GET %s responded %d %s. response size: %d%n",
                        url,
                        status.getStatusCode(),
                        status.getReasonPhrase(),
                        cw.toString().length());
                  } catch (IOException e) {
                    System.err.println("Error parsing response to " + url + ".");
                    e.printStackTrace();
                  } finally {
                    getResp.close();
                  }
                } catch (IOException e) {
                  System.err.println("Error sending request to " + url + ".");
                  e.printStackTrace();
                } finally {
                  try {
                    client.close();
                  } catch (Exception e) {
                    // nop
                  }
                }
                tc.trackDependency(
                    "FakeRestDependency", "fakeRestCommand", new Duration(123L), true);
                tc.trackEvent("FakeRestEvent");
                tc.trackMetric("FakeRestMetric", 1.0);
                tc.trackTrace("FakeRestTrace");
              }
            },
            "fakeRest operation")
        .run();
  }
}
