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

package com.microsoft.applicationinsights.agent.internal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Strings;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.internal.propagator.DelegatingPropagator;
import com.microsoft.applicationinsights.agent.internal.sampling.DelegatingSampler;
import io.opentelemetry.instrumentation.api.aisdk.AiLazyConfiguration;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LazyConfigurationAccessor implements AiLazyConfiguration.Accessor {

    private static final Logger logger = LoggerFactory.getLogger(LazyConfigurationAccessor.class);

    @Override
    public void lazyLoad() {
        lazySetEnvVars();
    }

    private void lazySetEnvVars() {
        String instrumentationKey = TelemetryConfiguration.getActive().getInstrumentationKey();
        String roleName = TelemetryConfiguration.getActive().getRoleName();
        if (instrumentationKey != null && !instrumentationKey.isEmpty() && roleName != null && !roleName.isEmpty()) {
            return;
        }

        boolean lazySetOptIn = Boolean.parseBoolean(System.getProperty("LazySetOptIn"));
        String enableAgent = System.getenv("APPLICATIONINSIGHTS_ENABLE_AGENT");
        logger.debug("lazySetOptIn: {}", lazySetOptIn);
        logger.debug("APPLICATIONINSIGHTS_ENABLE_AGENT: {}", enableAgent);
        if (!shouldSetConnectionString(lazySetOptIn, enableAgent)) {
            return;
        }

        setConnectionString(System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"), System.getenv("APPINSIGHTS_INSTRUMENTATIONKEY"));
        setWebsiteSiteName(System.getenv("WEBSITE_SITE_NAME"));
        String level = System.getenv("APPLICATIONINSIGHTS_SELF_DIAGNOSTICS_LEVEL");
        logger.info("####################### level-new: {}", level);
        setSelfDiagnosticsLevel(level);
    }

    static void setConnectionString(String connectionString, String instrumentationKey) {
        if (connectionString != null && !connectionString.isEmpty()) {
            setValue(connectionString);
        } else {
            // if the instrumentation key is neither null nor empty , we will create a default
            // connection string based on the instrumentation key.
            // this is to support Azure Functions that were created prior to the introduction of
            // connection strings
            if (instrumentationKey != null && !instrumentationKey.isEmpty()) {
                setValue("InstrumentationKey=" + instrumentationKey);
            }
        }
    }

    private static void setValue(String value) {
        if (!Strings.isNullOrEmpty(value)) {
            TelemetryConfiguration.getActive().setConnectionString(value);
            // now that we know the user has opted in to tracing, we need to init the propagator and sampler
            DelegatingPropagator.getInstance().setUpStandardDelegate();
            // TODO handle APPLICATIONINSIGHTS_SAMPLING_PERCENTAGE
            DelegatingSampler.getInstance().setAlwaysOnDelegate();
            logger.info("Set connection string {} lazily for the Azure Function Consumption Plan.", value);
        }
    }

    static void setWebsiteSiteName(String websiteSiteName) {
        if (websiteSiteName != null && !websiteSiteName.isEmpty()) {
            TelemetryConfiguration.getActive().setRoleName(websiteSiteName);
            logger.info("Set WEBSITE_SITE_NAME: {} lazily for the Azure Function Consumption Plan.", websiteSiteName);
        }
    }

    static void setSelfDiagnosticsLevel(String loggingLevel) {
        if (loggingLevel != null && !loggingLevel.isEmpty()) {
            logger.info("######################## logginglevel-new {}", loggingLevel);
            try {

//                ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
//                if (loggerFactory instanceof LoggerContext) {
//                    logger.info("####################### loggerFactory is an instance of LoggrContext");
//                    LoggerContext loggerContext = (LoggerContext) loggerFactory;
//                    if (loggerContext == null) {
//                        logger.error("####################### loggerContext is null");
//                    }
//                    logger.info("######################## loggerContext is not null");
//                    List<ch.qos.logback.classic.Logger> loggerList = loggerContext.getLoggerList();
//                    if (loggerList != null) {
//                        logger.info("######################## loggerList.size:", loggerList.size());
//                    } else {
//                        logger.info("######################## loggerList is null");
//                    }
//                    loggerList.stream().forEach(tmpLogger -> tmpLogger.setLevel(Level.DEBUG));
//                    logger.info("######################## logger.info should get logged");
//                    logger.debug("######################## logger debug should get logged.");
//                }
                // applicationinsights.extension.diagnostics
                // com.microsoft.applicationinsights.agent
                Logger startupLogger = LoggerFactory.getLogger("applicationinsights.extension.diagnostics");
                if (startupLogger instanceof ch.qos.logback.classic.Logger) {
                    logger.info("######################## applicationinsights.extension.diagnostics is a logback logger");
                    logger.info("######################## startupLogger.getname: {}", startupLogger.getName());
                    ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger)startupLogger;
                    logger.info("######################## logbacklogger.getName: {}", logbackLogger.getName());
                    logger.info("######################## Level.toLevel: {}", Level.toLevel(loggingLevel));
                    logbackLogger.setLevel(Level.DEBUG);
                    logger.debug("######################## debug should get logged.");
                    logbackLogger.debug("######################## logback logger debug should get logged.");
                } else {
                    logger.info("######################## applicationinsights.extension.diagnostics is not an instance of logback logger.");
                }

            } catch (Exception ex) {
                logger.error("######################## ex: {}", ex.getMessage());
                throw ex;
            }
        }

        logger.warn("######################## logger.warn should get logged");
    }

    static boolean shouldSetConnectionString(boolean lazySetOptIn, String enableAgent) {
        if (lazySetOptIn) {
            // when LazySetOptIn is on, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is null or true
            if (enableAgent == null || Boolean.parseBoolean(enableAgent)) {
                return true;
            }
        } else {
            // when LazySetOptIn is off, enable agent if APPLICATIONINSIGHTS_ENABLE_AGENT is true
            if (Boolean.parseBoolean(enableAgent)) {
                return true;
            }
        }
        return false;
    }
}
