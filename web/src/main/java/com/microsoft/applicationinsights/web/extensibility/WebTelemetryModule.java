package com.microsoft.applicationinsights.web.extensibility;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Created by yonisha on 2/2/2015.
 */
public interface WebTelemetryModule {
    /**
     * Begin request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    void onBeginRequest(ServletRequest req, ServletResponse res);

    /**
     * End request processing.
     * @param req The request to process
     * @param res The response to modify
     */
    void onEndRequest(ServletRequest req, ServletResponse res);
}