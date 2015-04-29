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

package com.microsoft.applicationinsights.management.authentication;

/**
 * Created by yonisha on 4/15/2015.
 */
public class Settings {
    public static String getTenant() {
        return "common";
    }

    public static String getRedirectURI() {
//        return  "https://portal.azure.com";
        return  "https://msopentech.com/";
    }

    public static String getClientId() {
        return "61d65f5a-6e3b-468b-af73-a033f5098c5c";

//        return "777acee8-5286-4d6e-8b05-f7c851d8ed0a";
    }

    public static String getAzureServiceManagementUri() {
        return "https://management.core.windows.net/";
    }

    public static String getAdAuthority() {
        return "login.windows.net";
    }

    public static String getResource() {
        return "https://management.core.windows.net/";
    }
}