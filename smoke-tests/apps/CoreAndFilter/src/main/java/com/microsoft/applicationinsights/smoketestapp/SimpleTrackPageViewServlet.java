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

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(
    description = "calls trackPageView twice; once vanilla, once with properties",
    urlPatterns = {"/trackPageView"})
public class SimpleTrackPageViewServlet extends HttpServlet {

  private final TelemetryClient client = new TelemetryClient();

  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    client.trackPageView("test-page");

    // just making sure flush() doesn't throw exception
    client.flush();

    PageViewTelemetry pvt2 = new PageViewTelemetry("test-page-2");
    // instrumentation key set on the Telemetry is used by interop
    pvt2.getContext().setInstrumentationKey("12341234-1234-1234-1234-123412341234");
    // role name and instance set on the Telemetry is used by interop
    pvt2.getContext().getCloud().setRole("role-goes-here");
    pvt2.getContext().getCloud().setRoleInstance("role-instance-goes-here");
    pvt2.getContext().getOperation().setName("operation-name-goes-here");
    pvt2.getContext().getOperation().setId("operation-id-goes-here");
    pvt2.getContext().getOperation().setParentId("operation-parent-id-goes-here");
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
    try {
      pvt2.setTimestamp(new SimpleDateFormat("dd/MM/yyyy").parse("10/10/2010"));
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
    pvt2.setDuration(123456);
    client.trackPageView(pvt2);

    TelemetryClient otherClient = new TelemetryClient();
    // instrumentation key set on the TelemetryClient is intentionally ignored by interop
    otherClient.getContext().setInstrumentationKey("12341234-1234-1234-1234-123412341234");
    // role name and instance set on the TelemetryClient are intentionally ignored by interop
    otherClient.getContext().getCloud().setRole("role-goes-here");
    otherClient.getContext().getCloud().setRoleInstance("role-instance-goes-here");
    otherClient.getContext().getOperation().setName("operation-name-goes-here");
    otherClient.getContext().getOperation().setId("operation-id-goes-here");
    otherClient.getContext().getOperation().setParentId("operation-parent-id-goes-here");
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
    try {
      pvt3.setTimestamp(new SimpleDateFormat("dd/MM/yyyy").parse("10/10/2010"));
    } catch (ParseException e) {
      throw new AssertionError(e);
    }
    pvt3.setDuration(123456);
    otherClient.trackPageView(pvt3);

    ServletFuncs.geRrenderHtml(request, response);
  }
}
