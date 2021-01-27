package com.microsoft.applicationinsights.internal.util;

import java.io.File;
import javax.net.ssl.SSLHandshakeException;

import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

public class SSLUtil {

    public static String getJavaCacertsPath() {
        String JAVA_HOME = System.getProperty("java.home");
        return new File(JAVA_HOME, "lib/security/cacerts").getPath();
    }

    public static String getSSLFriendlyExceptionBanner(String url) {
        if (url.equals(Defaults.LIVE_ENDPOINT)) {
            return "ApplicationInsights Java Agent failed to connect to Live metric end point.";
        }
        return "ApplicationInsights Java Agent failed to send telemetry data.";
    }

    public static String getSSLFriendlyExceptionMessage() {
        return "Unable to find valid certification path to requested target.";
    }

    public static String getSSLFriendlyExceptionAction(String completeUrl, boolean isUsingCustomKeyStore) {
        if (isUsingCustomKeyStore) {
            return "Please import the SSL certificate from " + completeUrl + ", into your custom java key store.\n" +
                    "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
        }
        return "Please import the SSL certificate from " + completeUrl + ", into the default java key store located at:\n"
                + SSLUtil.getJavaCacertsPath() + "\n"
                + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
    }

    public static String getSSLFriendlyExceptionNote() {
        return "This message is only logged the first time it occurs after startup.";
    }
}
