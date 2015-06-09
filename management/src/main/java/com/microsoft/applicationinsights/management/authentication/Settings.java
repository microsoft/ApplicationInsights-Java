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
    private static String tenant = "common"; // "2528489d-68e2-4819-9781-f11214b8b03c"; //72f988bf-86f1-41af-91ab-2d7cd011db47
    private static String redirectUrl = "https://portal.azure.com"; // https://portal.azure.com "https://msopentech.com/";
    private static String clientId = "777acee8-5286-4d6e-8b05-f7c851d8ed0a"; //777acee8-5286-4d6e-8b05-f7c851d8ed0a 61d65f5a-6e3b-468b-af73-a033f5098c5c

    public static String getTenant() {
        return tenant;
    }

    public static void setTenant(String newTenant) {
        tenant = newTenant;
    }

    public static String getRedirectURI() {
        return redirectUrl;
    }

    public static void setRedirectURI(String newUrl) {
        redirectUrl = newUrl;
    }

    public static String getClientId() {
        return clientId;
    }

    public static void setClientId(String newId) {
        clientId = newId;
    }

    public static String getAzureServiceManagementUri() {
        return "https://management.core.windows.net/";
    }

    public static String getAdAuthority() { return "login.windows.net"; }

    public static String getResource() {
        return "https://management.core.windows.net/";
    }
}