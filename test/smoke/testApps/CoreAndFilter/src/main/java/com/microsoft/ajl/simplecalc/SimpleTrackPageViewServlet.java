package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;

/**
 * Servlet implementation class SimpleTrackTraceServlet
 */
@WebServlet(description = "calls trackPageView twice; once vanilla, once with properties", urlPatterns = {"/trackPageView"})
public class SimpleTrackPageViewServlet extends HttpServlet {
    private static final long serialVersionUID = -633683109556605395L;
    private final TelemetryClient client = new TelemetryClient();

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        client.trackPageView("test-page");

        // just making sure flush() doesn't throw exception
        client.flush();

        PageViewTelemetry pvt2 = new PageViewTelemetry("test-page-2");
        pvt2.getContext().setInstrumentationKey("12341234-1234-1234-1234-123412341234");
        pvt2.getContext().getOperation().setName("operation-name-goes-here");
        pvt2.getContext().getUser().setId("user-id-goes-here");
        pvt2.getContext().getUser().setAccountId("account-id-goes-here");
        pvt2.getContext().getUser().setUserAgent("user-agent-goes-here");
        // don't set device id, because then tests fail with "Telemetry from previous container"
        // because they use device id to verify telemetry is from the current container
        pvt2.getContext().getDevice().setOperatingSystem("os-goes-here");
        pvt2.getContext().getSession().setId("session-id-goes-here");
        pvt2.getContext().getLocation().setIp("1.2.3.4");
        pvt2.getContext().getProperties().put("a-prop", "a-value");
        pvt2.getContext().getProperties().put("another-prop", "another-value");
        pvt2.getProperties().put("key", "value");
        pvt2.setDuration(123456);
        client.trackPageView(pvt2);

        TelemetryClient otherClient = new TelemetryClient();
        // instrumentation key set directly on the TelemetryClient is intentionally ignored by interop
        otherClient.getContext().setInstrumentationKey("12341234-1234-1234-1234-123412341234");
        otherClient.getContext().getOperation().setName("operation-name-goes-here");
        otherClient.getContext().getUser().setId("user-id-goes-here");
        otherClient.getContext().getUser().setAccountId("account-id-goes-here");
        otherClient.getContext().getUser().setUserAgent("user-agent-goes-here");
        // don't set device id, because then tests fail with "Telemetry from previous container"
        // because they use device id to verify telemetry is from the current container
        otherClient.getContext().getDevice().setOperatingSystem("os-goes-here");
        otherClient.getContext().getSession().setId("session-id-goes-here");
        otherClient.getContext().getLocation().setIp("1.2.3.4");
        otherClient.getContext().getProperties().put("a-prop", "a-value");
        otherClient.getContext().getProperties().put("another-prop", "another-value");
        PageViewTelemetry pvt3 = new PageViewTelemetry("test-page-3");
        pvt3.getProperties().put("key", "value");
        pvt3.setDuration(123456);
        otherClient.trackPageView(pvt3);

        ServletFuncs.geRrenderHtml(request, response);
    }
}