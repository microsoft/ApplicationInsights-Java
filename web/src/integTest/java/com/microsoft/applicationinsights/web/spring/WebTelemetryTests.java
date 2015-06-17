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

package com.microsoft.applicationinsights.web.spring;

import com.microsoft.applicationinsights.framework.ApplicationTelemetryManager;
import com.microsoft.applicationinsights.framework.HttpRequestClient;
import com.microsoft.applicationinsights.framework.TestEnvironment;
import com.microsoft.applicationinsights.framework.UriWithExpectedResult;
import com.microsoft.applicationinsights.framework.telemetries.DocumentType;
import com.microsoft.applicationinsights.framework.telemetries.RequestTelemetryItem;
import com.microsoft.applicationinsights.framework.telemetries.TelemetryItem;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.microsoft.applicationinsights.web.utils.HttpHelper;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.text.ParseException;
import java.util.*;

/**
 * Created by moralt on 05/05/2015.
 */
public class WebTelemetryTests {
    private String runId = LocalStringsUtils.generateRandomId(false);
    private static TestEnvironment testEnv;
    private static ApplicationTelemetryManager applicationTelemetryManager;

    @BeforeClass
    public static void classInitialization() throws Exception {
        testEnv = new TestEnvironment();
        applicationTelemetryManager = new ApplicationTelemetryManager(
                testEnv.getApplicationStorageConnectionString(), testEnv.getApplicationStorageExportQueueName());
    }

    /**
     * Sends GET requests to server and expects that will telemetry from app insights and it will include the correct information about the request
     * @throws Exception
     */
    @Test
    public void sendHttpRequestsTest() throws Exception {
        HashSet<TelemetryItem> expectedTelemetries = sendHttpGetRequests();
        HashSet<TelemetryItem> realTelemetries = applicationTelemetryManager.getApplicationTelemetries(DocumentType.Requests, expectedTelemetries.size());

        HashSet<TelemetryItem> missingTelemetry = getMismatchingTelemetryItems(realTelemetries, expectedTelemetries);
        if (missingTelemetry.size() > 0) {
            String errorRequests = "";
            for (TelemetryItem item : missingTelemetry) {
                errorRequests += "\n" + item.getProperty("uri");
                System.out.println("Didn't find matching item in real telemetry for request of URI " + item.getProperty("uri"));
            }

            Assert.fail("Didn't find match for " + missingTelemetry.size() + " items.\nError HTTP requests:" + errorRequests);
        }
    }

    private HashSet<TelemetryItem> sendHttpGetRequests() throws Exception {
        ArrayList<UriWithExpectedResult> uriPathsToRequest = new ArrayList<UriWithExpectedResult>();
        UriWithExpectedResult booksRequest =
                new UriWithExpectedResult("books?id=Thriller&runId=" + runId, HttpStatus.OK_200, "GET /bookstore-spring/books");
        UriWithExpectedResult loanRequest =
                new UriWithExpectedResult("loan?title=Gone%20Girl&id=030758836x&subject=Thriller&runId=" + runId, HttpStatus.OK_200, "GET /bookstore-spring/loan");
        UriWithExpectedResult nonExistingPageRequest =
                new UriWithExpectedResult("nonExistingWebFage?runId=" + runId, HttpStatus.NOT_FOUND_404, "GET /bookstore-spring/nonExistingWebFage");

        uriPathsToRequest.add(booksRequest);
        uriPathsToRequest.add(loanRequest);
        uriPathsToRequest.add(nonExistingPageRequest);

        HashSet<TelemetryItem> expectedTelemetries = new HashSet<TelemetryItem>();

        for (UriWithExpectedResult uriWithExpectedResult : uriPathsToRequest) {
            String requestId = LocalStringsUtils.generateRandomId(false);
            String uriWithRequestId = uriWithExpectedResult.getUri() + "&requestId=" + requestId;

            URI fullRequestUri = HttpRequestClient.constructUrl(
                    testEnv.getApplicationServer(),
                    testEnv.getApplicationServerPort(),
                    testEnv.getApplicationName(), uriWithRequestId);

            List<String> requestHeaders = generateUserAndSessionCookieHeader();
            int responseCode = HttpRequestClient.sendHttpRequest(fullRequestUri, requestHeaders);

            int expectedResponseCode = uriWithExpectedResult.getExpectedResponseCode();
            if (responseCode != expectedResponseCode) {
                String errorMessage = String.format(
                        "Unexpected response code '%s' for URI: %s. Expected: %s.", responseCode, uriWithExpectedResult.getUri(), expectedResponseCode);

                Assert.fail(errorMessage);
            }

            TelemetryItem expectedTelemetry = createExpectedResult(fullRequestUri, requestId, responseCode, uriWithExpectedResult.getExpectedRequestName());
            expectedTelemetries.add(expectedTelemetry);
        }

        return expectedTelemetries;
    }

    private List<String> generateUserAndSessionCookieHeader() throws ParseException {
        String formattedUserCookieHeader = HttpHelper.getFormattedUserCookieHeader();
        String formattedSessionCookie = HttpHelper.getFormattedSessionCookie(false);

        List<String> cookiesList = new ArrayList<String>();
        cookiesList.add(formattedUserCookieHeader);
        cookiesList.add(formattedSessionCookie);

        return cookiesList;
    }

    /**
     * Creates expected HTTP request result
     * @param uri The URI for the request
     * @param requestId UUID of the request
     * @param responseCode The expected response code for HTTP request with this URI
     * @return A TelemetryItem with the expected results
     */
    private TelemetryItem createExpectedResult(URI uri, String requestId, int responseCode, String requestName) {
        final String expectedUserAndSessionId = "00000000-0000-0000-0000-000000000000";

        TelemetryItem telemetryItem = new RequestTelemetryItem();
        telemetryItem.setProperty("id", requestId);
        telemetryItem.setProperty("port", Integer.toString(uri.getPort()));
        telemetryItem.setProperty("responseCode", Integer.toString(responseCode));
        telemetryItem.setProperty("uri", uri.toString());
        telemetryItem.setProperty("sessionId", expectedUserAndSessionId);
        telemetryItem.setProperty("userId", expectedUserAndSessionId);
        telemetryItem.setProperty("requestName", requestName);

        String[] params = uri.getQuery().split("&");
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            telemetryItem.setProperty("queryParameter." + name, value);
        }

        return telemetryItem;
    }

    /**
     * Tests if the expected telemetry exists in the real telemetry from AppInsights
     * @param applicationTelemetries The telemetry items that should contain all telemetry items in 'expectedTelemetries'
     * @param expectedTelemetries The telemetry items that should be contained in the 'applicationTelemetries'
     * @return A collection of telemetry items from 'expectedTelemetries' that are not in 'applicationTelemetries'
     */
    private HashSet<TelemetryItem> getMismatchingTelemetryItems(HashSet<TelemetryItem> applicationTelemetries,
                                                                HashSet<TelemetryItem> expectedTelemetries) {

        HashSet<TelemetryItem> missingTelemetry = new HashSet<TelemetryItem>();

        for (TelemetryItem item : expectedTelemetries) {
            if (!applicationTelemetries.contains(item)) {
                System.out.println("Missing expected telemetry item with document type " + item.getDocType());
                missingTelemetry.add(item);
            }
        }

        return missingTelemetry;
    }
}
