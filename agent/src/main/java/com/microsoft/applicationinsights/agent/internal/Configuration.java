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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.internal.config.AgentXmlElement;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement.InstrumentationXmlElement;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement.SessionTrackingXmlElement;
import com.microsoft.applicationinsights.internal.config.AgentXmlElement.UserTrackingXmlElement;
import com.microsoft.applicationinsights.internal.config.ParamXmlElement;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptor;
import org.glowroot.instrumentation.engine.config.InstrumentationDescriptors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    static Map<String, InstrumentationXmlElement> getInstrumentationXmlElements(AgentXmlElement agentXmlElement) {

        Map<String, InstrumentationXmlElement> instrumentationXmlElements = new HashMap<>();
        for (InstrumentationXmlElement instrumentationXmlElement : agentXmlElement.getInstrumentation()) {
            instrumentationXmlElements.put(instrumentationXmlElement.getName(), instrumentationXmlElement);
        }
        return instrumentationXmlElements;
    }

    static List<InstrumentationDescriptor> getInstrumentationDescriptors(
            Map<String, InstrumentationXmlElement> instrumentationXmlElements) throws IOException {

        List<InstrumentationDescriptor> instrumentationDescriptors = new ArrayList<>();

        for (InstrumentationDescriptor instrumentationDescriptor : InstrumentationDescriptors.read()) {
            InstrumentationXmlElement instrumentationXmlElement =
                    instrumentationXmlElements.get(instrumentationDescriptor.id());
            if (instrumentationXmlElement == null || instrumentationXmlElement.isEnabled()) {
                instrumentationDescriptors.add(instrumentationDescriptor);
            }
        }
        return instrumentationDescriptors;
    }

    static Map<String, Map<String, Object>> getInstrumentationConfig(AgentXmlElement agentXmlElement,
            Map<String, InstrumentationXmlElement> instrumentationXmlElements) {

        Map<String, Map<String, Object>> instrumentationConfig = new HashMap<>();

        Map<String, Object> servletConfig = new HashMap<>();
        servletConfig.put("captureRequestServerHostname", true);
        servletConfig.put("captureRequestServerPort", true);
        servletConfig.put("captureRequestScheme", true);
        List<String> cookieNames = new ArrayList<>();

        if (agentXmlElement.getUserTracking().isEnabled()) {
            cookieNames.add("ai_user");
        }
        if (agentXmlElement.getSessionTracking().isEnabled()) {
            cookieNames.add("ai_session");
        }
        if (!cookieNames.isEmpty()) {
            servletConfig.put("captureRequestCookies", cookieNames);
        }

        Map<String, Object> jdbcConfig = new HashMap<>();
        jdbcConfig.put("captureBindParametersIncludes", Collections.emptyList());
        jdbcConfig.put("captureResultSetNavigate", false);
        jdbcConfig.put("captureGetConnection", false);

        InstrumentationXmlElement jdbcXmlElement = instrumentationXmlElements.get("jdbc");
        if (jdbcXmlElement != null) {
            String explainPlanThresholdInMS = getParams(jdbcXmlElement).get("explainPlanThresholdInMS");
            if (explainPlanThresholdInMS != null) {
                try {
                    jdbcConfig.put("explainPlanThresholdMillis", Long.parseLong(explainPlanThresholdInMS));
                } catch (NumberFormatException e) {
                    logger.error("could not parse param explainPlanThresholdInMS: {}", explainPlanThresholdInMS, e);
                }
            }
        }

        Map<String, Object> springConfig = new HashMap<>();
        springConfig.put("useAltTransactionNaming", true);

        instrumentationConfig.put("servlet", servletConfig);
        instrumentationConfig.put("jdbc", jdbcConfig);
        instrumentationConfig.put("spring", springConfig);

        return instrumentationConfig;
    }

    private static Map<String, String> getParams(InstrumentationXmlElement instrumentationXmlElement) {

        Map<String, String> params = new HashMap<>();
        for (ParamXmlElement param : instrumentationXmlElement.getParams()) {
            params.put(param.getName(), param.getValue());
        }
        return params;
    }
}
