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

import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.agent.internal.utils.Global;
import com.microsoft.applicationinsights.internal.config.ApplicationInsightsXmlConfiguration;
import com.microsoft.applicationinsights.internal.config.ConfigurationFileLocator;
import com.microsoft.applicationinsights.internal.config.TelemetryConfigurationFactory;

class ApplicationInsightsXmlLoader {

    static ApplicationInsightsXmlConfiguration load(File applicationInsightsXmlFile) {
        String configDirPropName = ConfigurationFileLocator.CONFIG_DIR_PROPERTY;
        String propValue = System.getProperty(configDirPropName);
        try {
            System.setProperty(configDirPropName, applicationInsightsXmlFile.getParent());
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
}
