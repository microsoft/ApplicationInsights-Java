package com.microsoft.applicationinsights.internal.util;

import java.io.File;

import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.microsoft.applicationinsights.internal.config.connection.ConnectionString.Defaults;

public class SSLUtil {

    public static FriendlyException newSSLFriendlyException(String url)  {
        return new FriendlyException(getSSLFriendlyExceptionBanner(url),
                getSSLFriendlyExceptionMessage(),
                getSSLFriendlyExceptionAction(url),
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
        return null;
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

    private static String getSSLFriendlyExceptionAction(String url) {
        String customJavaKeyStorePath = getCustomJavaKeystorePath();
        if (customJavaKeyStorePath != null) {
            return "Please import the SSL certificate from " + url + ", into your custom java key store located at:\n"
                    + customJavaKeyStorePath + "\n"
                    + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
        }
        return "Please import the SSL certificate from " + url + ", into the default java key store located at:\n"
                + getJavaCacertsPath() + "\n"
                + "Learn more about importing the certificate here: https://go.microsoft.com/fwlink/?linkid=2151450";
    }

    private static String getSSLFriendlyExceptionNote() {
        return "This message is only logged the first time it occurs after startup.";
    }
}
