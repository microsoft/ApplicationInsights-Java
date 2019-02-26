package com.microsoft.applicationinsights.web.internal.httputils;

import org.apache.http.annotation.Experimental;

/**
 * Adapter Interface for handling information extraction from Http client and server.
 * @param <P> HttpRequest Entity
 * @param <Q> HttpResponse Entity
 */
@Experimental
public interface HttpExtractor<P /* >>> extends @NonNull Object*/, Q> {

    /**
     * Return the URL from HttpRequest
     * @param request HttpRequest Entity
     * @return URL String
     */
    String getUniformResourceLocator(P request);

    /**
     * Returns the HTTP method like - GET, POST etc.
     * @param request Http Request Entity
     * @return method string
     */
    String getMethod(P request);

    /**
     * Returns the host of the incoming request
     * @param request HttpRequest Entity
     * @return Host String
     */
    String getHost(P request);

    /**
     * Returns the query path for request
     * @param request HttpRequest Entity
     * @return Query Path String
     */
    String getQuery(P request);

    /**
     * Returns the path for the request url
     * @param request HttpRequest entity
     * @return Path string
     */
    String getPath(P request);

    /**
     * Returns the value of user-agent header
     * @param request HttpRequest entity
     * @return user-agent header string
     */
    String getUserAgent(P request);

    /**
     * Returns the status code of request. Returns 0 if no response is available.
     * @param response HttpResponse entity
     * @return response code integer
     */
    int getStatusCode(Q response);

    /**
     * Returns the uri of the given request
     * @param request HttpRequest entity
     * @return uri string
     */
    String getUniformResourceIdentifier(P request);

    /**
     * Returns the scheme of the given request
     * @param request  HttpRequest entity
     * @return scheme string
     */
    String getScheme(P request);
}