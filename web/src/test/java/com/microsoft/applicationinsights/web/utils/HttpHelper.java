/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.web.utils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by yonisha on 2/2/2015.
 */
public class HttpHelper {
    public static void sendGetRequestAndWait(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) (new URL(url)).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.4; WOW64; Trident/7.0; Touch; rv:11.0) like Gecko");
        int responseCode = con.getResponseCode();

        try {
            InputStream inputStream = con.getInputStream();
            while (inputStream.read() > 0) {
            }
            inputStream.close();
        } catch (Exception exc) {
        }

        try {
            InputStream errorStream = con.getErrorStream();
            while (errorStream .read() > 0) {}
            errorStream .close();
        } catch (Exception exc) {
        }

        //wait a little to allow server to complete request
        Thread.sleep(1000);

        System.out.println("Sent GET request to: " + url);
        System.out.println("Response Code: " + responseCode);
    }
}