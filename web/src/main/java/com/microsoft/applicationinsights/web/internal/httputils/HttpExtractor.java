package com.microsoft.applicationinsights.web.internal.httputils;

public interface HttpExtractor<P /* >>> extends @NonNull Object*/, Q> {

    String getUrl(P request);

    String getMethod(P request);

    String getHost(P request);

    String getQuery(P request);

    String getPath(P request);

    String getUserAgent(P request);

    int getStatusCode(Q response);

}
