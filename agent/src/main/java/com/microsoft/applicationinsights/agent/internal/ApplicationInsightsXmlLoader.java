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

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.config.AddTypeXmlElement;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.ConfigurationFileLocator;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;
import com.microsoft.applicationinsights.internal.config.TelemetryModulesXmlElement;
import com.microsoft.applicationinsights.web.internal.correlation.TraceContextCorrelationCore;

class ApplicationInsightsXmlLoader {

    private static final String W3C_CONFIGURATION_PARAMETER = "W3CEnabled";

    private static final String W3C_BACKCOMPAT_PARAMETER = "enableW3CBackCompat";

    static ApplicationInsightsXmlConfiguration load(File agentJarFile) {
        String configDirPropName = ConfigurationFileLocator.CONFIG_DIR_PROPERTY;
        String propValue = System.getProperty(configDirPropName);
        try {
            System.setProperty(configDirPropName, agentJarFile.getParent());
            return TelemetryConfigurationFactory.INSTANCE.twoPhaseInitializePart1();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            if (propValue == null) {
                System.clearProperty(configDirPropName);
            } else {
                System.setProperty(configDirPropName, propValue);
            }
        }
    }

    static TelemetryConfiguration load(ApplicationInsightsXmlConfiguration xmlConfiguration) {
        TelemetryConfiguration configuration = TelemetryConfiguration.getActiveWithoutInitializingConfig();
        TelemetryConfigurationFactory.INSTANCE.twoPhaseInitializePart2(configuration, xmlConfiguration);
        configuration.getContextInitializers().add(new Global.CloudRoleContextInitializer());
        return configuration;
    }

    static ExtraConfiguration removeBuiltInModules(ApplicationInsightsXmlConfiguration xmlConfiguration) {
        boolean userTracking = false;
        boolean sessionTracking = false;
        TelemetryModulesXmlElement modules = xmlConfiguration.getModules();
        if (modules == null) {
            return new ExtraConfiguration(false, false);
        }
        for (Iterator<AddTypeXmlElement> i = xmlConfiguration.getModules().getAdds().iterator(); i.hasNext(); ) {
            AddTypeXmlElement module = i.next();
            Map<String, String> data = module.getData();
            if (module.getType().equals(
                    "com.microsoft.applicationinsights.web.extensibility.modules.WebRequestTrackingTelemetryModule")) {
                if (data.containsKey(W3C_CONFIGURATION_PARAMETER)) {
                    Global.setInboundW3CEnabled(Boolean.valueOf(data.get(W3C_CONFIGURATION_PARAMETER)));
                }
                if (data.containsKey(W3C_BACKCOMPAT_PARAMETER)) {
                    TraceContextCorrelationCore
                            .setIsInboundW3CBackCompatEnabled(Boolean.valueOf(data.get(W3C_BACKCOMPAT_PARAMETER)));
                }
                i.remove();
            } else if (module.getType().equals(
                    "com.microsoft.applicationinsights.web.extensibility.modules.WebUserTrackingTelemetryModule")) {
                userTracking = true;
                i.remove();
            } else if (module.getType().equals(
                    "com.microsoft.applicationinsights.web.extensibility.modules.WebSessionTrackingTelemetryModule")) {
                sessionTracking = true;
                i.remove();
            }
        }
        return new ExtraConfiguration(userTracking, sessionTracking);
    }

    static class ExtraConfiguration {

        final boolean userTracking;
        final boolean sessionTracking;

        ExtraConfiguration(boolean userTracking, boolean sessionTracking) {
            this.userTracking = userTracking;
            this.sessionTracking = sessionTracking;
        }
    }
}
