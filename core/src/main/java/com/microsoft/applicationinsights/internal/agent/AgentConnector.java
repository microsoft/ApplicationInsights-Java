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

package com.microsoft.applicationinsights.internal.agent;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.ThreadLocalCleaner;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The class is responsible for connecting the Agent and register there for future calls
 *
 * Created by gupele on 5/7/2015.
 */
public enum AgentConnector {
    INSTANCE;

    enum RegistrationType {
        NONE,
        WEB,
        SELF
    }

    private String agentKey;
    private RegistrationType registrationType = RegistrationType.NONE;
    private CoreAgentNotificationsHandler coreDataAgent = null;

    public static class RegistrationResult {
        private final String key;
        private final ThreadLocalCleaner cleaner;

        public RegistrationResult(String key, ThreadLocalCleaner cleaner) {
            this.key = key;
            this.cleaner = cleaner;
        }

        public String getKey() {
            return key;
        }

        public ThreadLocalCleaner getCleaner() {
            return cleaner;
        }
    }

    /**
     * Registers the caller, and returning a key to represent that data. The method should not throw!
     *
     * The method is basically delegating the call to the relevant Agent class.
     *
     * @param classLoader The class loader that is associated with the caller.
     * @param name The name that is associated with the caller
     * @return The key that will represent the caller, null if the registration failed.
     */
    @SuppressWarnings("unchecked")
    public synchronized RegistrationResult register(ClassLoader classLoader, String name) {
        switch (registrationType) {
            case NONE:
                try {
                    coreDataAgent = new CoreAgentNotificationsHandler(name);
                    agentKey = ImplementationsCoordinator.INSTANCE.register(classLoader, coreDataAgent);
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.error( "Could not find Agent: '%s'", ExceptionUtils.getStackTrace(t));
                    agentKey = null;
                }

                registrationType = RegistrationType.WEB;
                return new RegistrationResult(agentKey, coreDataAgent.getCleaner());

            case WEB:
                return new RegistrationResult(agentKey, coreDataAgent.getCleaner());

            case SELF:
                InternalLogger.INSTANCE.error("Core was already registered by the Agent");
                return null;

            default:
                InternalLogger.INSTANCE.error("Unknown registration type '%s' found", registrationType);
                return null;
        }
    }

    /**
     * Registers the caller, and returning a key to represent that data. The method should not throw!
     *
     * The method is basically delegating the call to the relevant Agent class.
     * @return A boolean value representing the agent registration
     */
    @SuppressWarnings("unchecked")
    public synchronized boolean registerSelf() {
        switch (registrationType) {
            case NONE:
                try {
                    coreDataAgent = new CoreAgentNotificationsHandler("app");
                } catch (Throwable t) {
                    InternalLogger.INSTANCE.error("Could not find Agent: '%s'", ExceptionUtils.getStackTrace(t));
                    return false;
                }
                ImplementationsCoordinator.INSTANCE.registerSelf(coreDataAgent);

                registrationType = RegistrationType.SELF;

                return true;

            case WEB:
                InternalLogger.INSTANCE.error("Core was already registered by the Web module");
                return false;

            case SELF:
                InternalLogger.INSTANCE.info( "Core was already registered by the Agent, ignored");
                return true;

            default:
                InternalLogger.INSTANCE.error("Unknown registration type '%s' found", registrationType);
                return false;
        }
    }
}
