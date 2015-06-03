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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.FilterRegistration;

import com.microsoft.applicationinsights.internal.agent.AgentConnector;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.google.common.base.Strings;

/**
 * Created by gupele on 5/7/2015.
 */
//@WebListener
public final class WebAppInitializer implements ServletContextListener {

    private final static String FILTER_NAME = "ApplicationInsightsWebFilter";
    private final static String WEB_INF_FOLDER = "WEB-INF/";

    private String name;

    /**
     * The method is called by the container before the WebApp is initialized
     * The method will fetch the WebApp name and then will register itself with that name
     * The registration should return a 'key' which is later used to identify threads
     * that are originating from this WebApp. To do so, the class will create a filter
     * and pass the 'key' to that filter, the filter is responsible for assigning the 'key' to each thread.
     *
     * Note, that the method will take care of situations where the filter is already declared in the 'web.xml'
     *
     * @param sce The Servlet Context Event that we use to extract the data we need.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            ServletContext context = sce.getServletContext();

            name = getName(context);

            String key = registerWebApp(name);

            addFilter(context);

            WebRequestTrackingFilter.setName(key);

            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Successfully registered the filter '%s'", FILTER_NAME);
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to register '%s', exception: '%s'", FILTER_NAME, t.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    private void addFilter(ServletContext context) {
        FilterRegistration filterData = getAIFilter(context);
        if (filterData == null) {
            // Adding the filter since we didn't find that in configuration.

            WebRequestTrackingFilter filter = new WebRequestTrackingFilter();
            FilterRegistration.Dynamic filterRegistration = context.addFilter(FILTER_NAME, filter);

            if (filterRegistration == null) {
                // This should not happen
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Failed to add filter '%s'", FILTER_NAME);

                return;
            }

            filterRegistration.addMappingForUrlPatterns(null, false, "/*");
        } else {
            filterData.addMappingForUrlPatterns(null, false, "/*");
        }
    }

    private String registerWebApp(String name) {
        String key = null;

        if (!Strings.isNullOrEmpty(name)) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Registering WebApp with name '%s'", name);
            key = AgentConnector.INSTANCE.register(this.getClass().getClassLoader(), name);
            if (Strings.isNullOrEmpty(key)) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Key for '%s' key is null'. No way to have RDD telemetries for this WebApp", name);
            } else {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Registered WebApp '%s' key='%s'", name, key);
            }
        } else {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "WebApp name is not found, unable to register WebApp");
        }

        return key;
    }

    private FilterRegistration getAIFilter(ServletContext context){
        String filterClassName = WebRequestTrackingFilter.class.getName();

        Collection<? extends FilterRegistration> filterRegistrations = context.getFilterRegistrations().values();
        for ( FilterRegistration filterRegistration : filterRegistrations) {
            if (filterClassName.equals(filterRegistration.getClassName())) {
                InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "AI-Filter is already defined");

                return filterRegistration;
            }
        }

        InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.INFO, "Did not find AI-Filter");
        return null;
    }

    private String getName(ServletContext context) {
        String name = null;
        try {
            String contextPath = context.getContextPath();
            if (Strings.isNullOrEmpty(contextPath)) {
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
        } catch (Throwable t) {
            InternalLogger.INSTANCE.logAlways(InternalLogger.LoggingLevel.ERROR, "Exception while fetching WebApp name: '%s'", t.getMessage());
        }

        return name;
    }
}
