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

package com.microsoft.applicationinsights.web.internal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.LinkedList;

import javax.servlet.Filter;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.common.CommonUtils;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.coresync.impl.AgentTLS;
import com.microsoft.applicationinsights.internal.agent.AgentConnector;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ThreadLocalCleaner;

/**
 * Created by yonisha on 2/2/2015.
 */
public final class WebRequestTrackingFilter implements Filter {
    private final static String FILTER_NAME = "ApplicationInsightsWebFilter";
    private final static String WEB_INF_FOLDER = "WEB-INF/";

    private WebModulesContainer webModulesContainer;
    private boolean isInitialized = false;
    private TelemetryClient telemetryClient;
    private String key;
    private boolean agentIsUp = false;
    private final LinkedList<ThreadLocalCleaner> cleaners = new LinkedList<ThreadLocalCleaner>();
    private String appName;

    // endregion Members

    // region Public

    /**
     * Processing the given request and response.
     * @param req The servlet request.
     * @param res The servlet response.
     * @param chain The filters chain
     * @throws IOException Exception that can be thrown from invoking the filters chain.
     * @throws ServletException Exception that can be thrown from invoking the filters chain.
     */
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        ApplicationInsightsHttpResponseWrapper response = new ApplicationInsightsHttpResponseWrapper((HttpServletResponse)res);
        setKeyOnTLS(key);

        boolean isRequestProcessedSuccessfully = invokeSafeOnBeginRequest(req, response);

        try {
            chain.doFilter(req, response);
            invokeSafeOnEndRequest(req, response, isRequestProcessedSuccessfully);
        } catch (ServletException se) {
            onException(se, req, response,isRequestProcessedSuccessfully);
            throw se;
        } catch (IOException ioe) {
            onException(ioe, req, response, isRequestProcessedSuccessfully);
            throw ioe;
        } catch (RuntimeException re) {
            onException(re, req, response, isRequestProcessedSuccessfully);
            throw re;
        } finally {
            cleanup();
        }
    }

    public WebRequestTrackingFilter(String appName) {
        this.appName = appName;
    }

    private void cleanup() {
        try {
            ThreadContext.remove();

            setKeyOnTLS(null);
            for (ThreadLocalCleaner cleaner : cleaners) {
                cleaner.clean();
            }
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
        }
    }

    private void onException(Exception e, ServletRequest req, ServletResponse res, boolean isRequestProcessedSuccessfully) {
        try {
            InternalLogger.INSTANCE.trace("Unhandled application exception: %s", e.getMessage());
            if (telemetryClient != null) {
                telemetryClient.trackException(e);
            }
        } catch (Exception ignoreMe) {
        }
        invokeSafeOnEndRequest(req, res, isRequestProcessedSuccessfully);
    }

    /**
     * Initializes the filter from the given config.
     * @param config The filter configuration.
     */
    public void init(FilterConfig config){
        try {
            initialize(config);

            TelemetryConfiguration configuration = TelemetryConfiguration.getActive();

            if (configuration == null) {
                InternalLogger.INSTANCE.error(
                        "Java SDK configuration cannot be null. Web request tracking filter will be disabled.");

                return;
            }

            telemetryClient = new TelemetryClient(configuration);
            webModulesContainer = new WebModulesContainer(configuration);
            isInitialized = true;
        } catch (Exception e) {
            String filterName = this.getClass().getSimpleName();
            InternalLogger.INSTANCE.info(
                    "Application Insights filter %s has been failed to initialized.\n" +
                            "Web request tracking filter will be disabled. Exception: %s", filterName, e.getMessage());
        }
    }

    /**
     * Destroy the filter by releases resources.
     */
    public void destroy() {
        //add code to release any resource
    }

    // endregion Public

    // region Private

    private boolean invokeSafeOnBeginRequest(ServletRequest req, ServletResponse res) {
        if (!isInitialized) {
            return false;
        }
        boolean success = true;

        try {
            RequestTelemetryContext context = new RequestTelemetryContext(new Date().getTime(), (HttpServletRequest)req);
            ThreadContext.setRequestTelemetryContext(context);

            webModulesContainer.invokeOnBeginRequest(req, res);
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke OnBeginRequest on telemetry modules with the following exception: %s", e.getMessage());

            success = false;
        }

        return success;
    }

    private void invokeSafeOnEndRequest(ServletRequest req, ServletResponse res, boolean inProgress) {
        try {
            if (isInitialized && inProgress) {
                webModulesContainer.invokeOnEndRequest(req, res);
            }
        } catch (Exception e) {
            InternalLogger.INSTANCE.error(
                    "Failed to invoke OnEndRequest on telemetry modules with the following exception: %s", e.getMessage());
        }
    }

    private void setKeyOnTLS(String key) {
        if (agentIsUp) {
            try {
                AgentTLS.setTLSKey(key);
            } catch (Throwable e) {
                if (e instanceof ClassNotFoundException ||
                        e instanceof NoClassDefFoundError) {

                    // This means that the Agent is not present and therefore we will stop trying
                    agentIsUp = false;
                    InternalLogger.INSTANCE.error("setKeyOnTLS: Failed to find AgentTLS: '%s'", e.getMessage());
                }
            }
        }
    }

    public WebRequestTrackingFilter() {
    }

    private synchronized void initialize(FilterConfig filterConfig) {
        try {

            //if agent is not installed (jar not loaded), can skip the entire registration process
            try {
                AgentConnector test = AgentConnector.INSTANCE;
            } catch(Throwable t) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Agent was not found. Skipping the agent registration");
                return;
            }

            ServletContext context = filterConfig.getServletContext();

            String name = getName(context);

            String key = registerWebApp(appName);
            setKey(key);

            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Successfully registered the filter '%s'", FILTER_NAME);
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to register '%s', exception: '%s'", FILTER_NAME, t.getMessage());
        }
    }

    private String registerWebApp(String name) {
        String key = null;

        if (!CommonUtils.isNullOrEmpty(name)) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Registering WebApp with name '%s'", name);
            AgentConnector.RegistrationResult result = AgentConnector.INSTANCE.register(this.getClass().getClassLoader(), name);
            if (result == null) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Did not get a result when registered '%s'. No way to have RDD telemetries for this WebApp", name);
            }
            key = result.getKey();

            if (CommonUtils.isNullOrEmpty(key)) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Key for '%s' key is null'. No way to have RDD telemetries for this WebApp", name);
            } else {
                if (result.getCleaner() != null) {
                    cleaners.add(result.getCleaner());
                }
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Registered WebApp '%s' key='%s'", name, key);
            }
        } else {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "WebApp name is not found, unable to register WebApp");
        }

        return key;
    }

    private String getName(ServletContext context) {
        if (appName != null) {
            return appName;
        }
        String name = null;
        try {
            String contextPath = context.getContextPath();
            if (CommonUtils.isNullOrEmpty(contextPath)) {
                URL[] jarPaths = ((URLClassLoader) (this.getClass().getClassLoader())).getURLs();
                for (URL url : jarPaths) {
                    String urlPath = url.getPath();
                    int index = urlPath.lastIndexOf(WEB_INF_FOLDER);
                    if (index != -1) {
                        urlPath = urlPath.substring(0, index);
                        String[] parts = urlPath.split("/");
                        if (parts.length > 0) {
                            name = parts[parts.length - 1];
                            break;
                        }
                    }
                }
            } else {
                name = contextPath.substring(1);
            }
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Exception while fetching WebApp name: '%s'", t.getMessage());
        }
        appName = name;
        return name;
    }

    private void setKey(String key) {
        if (CommonUtils.isNullOrEmpty(key)) {
            agentIsUp = false;
            this.key = key;
            return;
        }

        try {
            AgentTLS.getTLSKey();
            agentIsUp = true;
            this.key = key;
        } catch (Throwable throwable) {
            agentIsUp = false;
            this.key = null;
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "setKey: Failed to find AgentTLS");
        }
    }
}
