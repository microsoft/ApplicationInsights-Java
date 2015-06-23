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

package com.microsoft.applicationinsights.test.framework;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created by yonisha on 6/16/2015.
 */
public class HttpRequestClient {

    // URI structure: http://<server>:<port>/<app_name>/<path>
    private static final String REQUEST_URI_TEMPLATE = "http://%s:%s/%s/%s";
    private static final String COOKIE_HEADER_KEY = "Cookie";

    private HttpRequestClient() {
    }

    public static int sendHttpRequest(URI uri, List<String> requestCookies) throws Exception {

        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();

        if (requestCookies != null && requestCookies.size() > 0) {
            String allCookies = "";

            for (String cookie : requestCookies) {
                allCookies = allCookies.concat(cookie + "; ");
            }

            allCookies = allCookies.substring(0, allCookies.length() - 2);
            connection.setRequestProperty(COOKIE_HEADER_KEY, allCookies);
        }

        System.out.println("Sending 'GET' request to URL: " + uri.toString());
        int responseCode = connection.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        return responseCode;
    }

    public static URI constructUrl(String server, int port, String app, String path) throws URISyntaxException {
        String url = String.format(REQUEST_URI_TEMPLATE, server, port, app, path);

        return new URI(url);
    }
}