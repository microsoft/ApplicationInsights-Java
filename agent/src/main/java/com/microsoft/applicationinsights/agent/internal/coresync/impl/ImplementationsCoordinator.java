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

package com.microsoft.applicationinsights.agent.internal.coresync.impl;

import java.net.URL;
import java.sql.Statement;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import com.microsoft.applicationinsights.agent.internal.agent.StringUtils;
import com.microsoft.applicationinsights.agent.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.agent.internal.coresync.AgentNotificationsHandler;

/**
 * The class serves as the contact point between injected code and its real implementation.
 * Injected code will notify on various events in the class, calling this class, the class
 * will then delegate the call to the relevant 'client' by consulting the data on the Thread's TLS
 *
 * Note that the called methods should not throw any exception or error otherwise that might affect user's code
 *
 * Created by gupele on 5/6/2015.
 */
public enum ImplementationsCoordinator implements AgentNotificationsHandler {
    INSTANCE;

    @Override
    public void onException(String className, String methodName, Throwable throwable) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onException(className, methodName, throwable);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onMethodEnterURL(String name, URL url) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onMethodEnterURL(name, url);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onMethodEnterSqlStatement(String name, Statement statement, String sqlStatement) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onMethodEnterSqlStatement(name, statement, sqlStatement);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onDefaultMethodEnter(String name) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onDefaultMethodEnter(name);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onMethodFinish(String name, Throwable throwable) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onMethodFinish(name, throwable);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public void onMethodFinish(String name) {
        try {
            AgentNotificationsHandler implementation = getImplementation();
            if (implementation != null) {
                implementation.onMethodFinish(name);
            }
        } catch (Throwable t) {
        }
    }

    @Override
    public String getName() {
        return null;
    }

    /**
     * The data we expect to have for every thread
     */
    public static class RegistrationData {
        public final ClassLoader classLoader;
        public final AgentNotificationsHandler handler;
        public final String key;

        public RegistrationData(ClassLoader classLoader, AgentNotificationsHandler handler, String key) {
            this.classLoader = classLoader;
            this.handler = handler;
            this.key = key;
        }
    }

    private static ConcurrentHashMap<String, RegistrationData> implementations = new ConcurrentHashMap<String, RegistrationData>();

    public String register(ClassLoader classLoader, AgentNotificationsHandler handler) {
        try {
            if (handler == null) {
                throw new IllegalArgumentException("AgentNotificationsHandler must be a non-null value");
            }

            String implementationName = handler.getName();
            if (StringUtils.isNullOrEmpty(implementationName)) {
                throw new IllegalArgumentException("AgentNotificationsHandler name must have be a non-null non empty value");
            }

            implementations.put(implementationName, new RegistrationData(classLoader, handler, implementationName));

            return implementationName;
        } catch (Throwable throwable) {
            InternalLogger.INSTANCE.error("Exception: '%s'", throwable.getMessage());
            return null;
        }
    }

    public Collection<RegistrationData> getRegistered() {
        return implementations.values();
    }

    private AgentNotificationsHandler getImplementation() {
        String key = AgentTLS.getTLSKey();
        if (key != null && key.length() > 0) {
            RegistrationData implementation = implementations.get(key);
            if (implementation != null) {
                return implementation.handler;
            }
        }

        return null;
    }
}
