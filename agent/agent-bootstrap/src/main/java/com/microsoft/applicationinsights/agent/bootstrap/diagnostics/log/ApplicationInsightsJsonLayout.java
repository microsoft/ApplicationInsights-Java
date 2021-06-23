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
package com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.ApplicationMetadataFactory;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsValueFinder;

public class ApplicationInsightsJsonLayout extends JsonLayout {

    public static final String TIMESTAMP_PROP_NAME = "time";
    public static final String OPERATION_NAME_PROP_NAME = "operation";
    public static final String CUSTOM_FIELDS_PROP_NAME = "properties";

    // visible for testing
    static final String UNKNOWN_VALUE = "unknown";

    // visible for testing
    final List<DiagnosticsValueFinder> valueFinders = new ArrayList<>();


    public ApplicationInsightsJsonLayout() {
        ApplicationMetadataFactory mf = DiagnosticsHelper.getMetadataFactory();
        valueFinders.add(mf.getSiteName());
        valueFinders.add(mf.getInstrumentationKey());
        valueFinders.add(mf.getExtensionVersion());
        valueFinders.add(mf.getSdkVersion());
        valueFinders.add(mf.getSubscriptionId());
    }

    @Override
    protected Map toJsonMap(ILoggingEvent event) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        addTimestamp(TIMESTAMP_PROP_NAME, true, event.getTimeStamp(), jsonMap);
        add(LEVEL_ATTR_NAME, true, String.valueOf(event.getLevel()), jsonMap);
        add(LOGGER_ATTR_NAME, true, event.getLoggerName(), jsonMap);
        add(FORMATTED_MESSAGE_ATTR_NAME, true, event.getFormattedMessage(), jsonMap);
        addThrowableInfo(EXCEPTION_ATTR_NAME, true, event, jsonMap);
        addMap(CUSTOM_FIELDS_PROP_NAME, true, getPropertiesMap(event), jsonMap);
        return jsonMap;
    }

    private Map<String, Object> getPropertiesMap(ILoggingEvent event) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        add(OPERATION_NAME_PROP_NAME, true, getOperationName(event), jsonMap);
        for (DiagnosticsValueFinder finder : valueFinders) {
            String value = finder.getValue();
            add(finder.getName(), true, value == null || value.isEmpty() ? UNKNOWN_VALUE : value, jsonMap);
        }
        return jsonMap;
    }

    public String getOperationName(ILoggingEvent event) {
        return event.getMDCPropertyMap().get(DiagnosticsHelper.MDC_PROP_OPERATION);
    }

}
