package com.microsoft.applicationinsights.internal.util;

import java.io.File;

import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

public class SSLUtil {

    public static void throwSSLFriendlyException(String url) {
        boolean isUsingCustomKeyStore = (System.getProperty("javax.net.ssl.trustStore") != null);
        throw new FriendlyException(getSSLFriendlyExceptionBanner(url),
                getSSLFriendlyExceptionMessage(),
                getSSLFriendlyExceptionAction(url, isUsingCustomKeyStore),
                getSSLFriendlyExceptionNote());
    }
    private static String getJavaCacertsPath() {
        String JAVA_HOME = System.getProperty("java.home");
        return new File(JAVA_HOME, "lib/security/cacerts").getPath();
    }

    private static String getCustomJavaKeystorePath() {
        String cacertsPath = System.getProperty("javax.net.ssl.trustStore");
        if(cacertsPath!=null) {
            return new File(cacertsPath).getPath();
        }
        return "Custom Java Keystore Path not specified";
    }


    private static String getSSLFriendlyExceptionBanner(String url) {
        if (url.equals(Defaults.LIVE_ENDPOINT)) {
            return "ApplicationInsights Java Agent failed to connect to Live metric end point.";
        }
        return "ApplicationInsights Java Agent failed to send telemetry data.";
    }

    private static String getSSLFriendlyExceptionMessage() {
        return "Unable to find valid certification path to requested target.";
    }

    private static String getSSLFriendlyExceptionAction(String completeUrl, boolean isUsingCustomKeyStore) {
        if (isUsingCustomKeyStore) {
            return "Please import the SSL certificate from " + completeUrl + ", into your custom java key store located at:\n"
                    + getCustomJavaKeystorePath() + "\n"
                    + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
        }
        return "Please import the SSL certificate from " + completeUrl + ", into the default java key store located at:\n"
                + getJavaCacertsPath() + "\n"
                + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
    }

    private static String getSSLFriendlyExceptionNote() {
        return "This message is only logged the first time it occurs after startup.";
    }
}
