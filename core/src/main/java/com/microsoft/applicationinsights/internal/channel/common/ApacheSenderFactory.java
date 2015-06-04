package com.microsoft.applicationinsights.internal.channel.common;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.reflect.ClassDataUtils;

/**
 * Created by gupele on 6/4/2015.
 */
final class ApacheSenderFactory {
    public ApacheSender create() {
        if (!ClassDataUtils.INSTANCE.verifyClassExists("org.apache.http.conn.HttpClientConnectionManager")) {
            String errorMessage = String.format("Found an old version of HttpClient jar, for best performance consider upgrading to version 4.3+");

            InternalLogger.INSTANCE.warn(errorMessage);

            return new ApacheSender42();
        }

        InternalLogger.INSTANCE.trace("Using Http Client version 4.3+");
        return new ApacheSender43();
    }
}
