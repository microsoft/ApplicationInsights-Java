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