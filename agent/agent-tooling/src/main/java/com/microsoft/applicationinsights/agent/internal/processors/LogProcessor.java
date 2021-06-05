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

package com.microsoft.applicationinsights.agent.internal.processors;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.applicationinsights.agent.internal.processors.ProcessorUtil.*;


public class LogProcessor extends AgentProcessor {
    private final List<AttributeKey<?>> fromAttributes;
    private final List<Pattern> toAttributeRulePatterns;
    private final List<List<String>> groupNames;
    private final String separator;

    public LogProcessor(@Nullable IncludeExclude include,
                        @Nullable IncludeExclude exclude,
                        List<AttributeKey<?>> fromAttributes,
                        List<Pattern> toAttributeRulePatterns,
                        List<List<String>> groupNames,
                        String separator) {
        super(include, exclude);
        this.fromAttributes = fromAttributes;
        this.toAttributeRulePatterns = toAttributeRulePatterns;
        this.groupNames = groupNames;
        this.separator = separator;
    }

    public static LogProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<AttributeKey<?>> fromAttributes = new ArrayList<>();
        if (config.body.fromAttributes != null) {
            for (String attribute : config.body.fromAttributes) {
                fromAttributes.add(AttributeKey.stringKey(attribute));
            }
        }
        List<String> toAttributeRules = new ArrayList<>();
        if (config.body.toAttributes != null) {
            toAttributeRules.addAll(config.body.toAttributes.rules);
        }
        List<Pattern> toAttributeRulePatterns = new ArrayList<>();
        if (config.body.toAttributes != null) {
            for (String rule : config.body.toAttributes.rules) {
                toAttributeRulePatterns.add(Pattern.compile(rule));
            }
        }
        List<List<String>> groupNames = getGroupNamesList(toAttributeRules);
        String separator = config.body.separator != null ? config.body.separator : "";
        return new LogProcessor(normalizedInclude, normalizedExclude,
                fromAttributes, toAttributeRulePatterns, groupNames, separator);
    }

    //fromAttributes represents the attribute keys to pull the values from to generate the new span name.
    public SpanData processFromAttributes(SpanData log) {
        if (spanHasAllFromAttributeKeys(log, fromAttributes)) {
            StringBuilder updatedSpanBuffer = new StringBuilder();
            Attributes existingSpanAttributes = log.getAttributes();
            for (AttributeKey<?> attributeKey : fromAttributes) {
                updatedSpanBuffer.append(existingSpanAttributes.get(attributeKey));
                updatedSpanBuffer.append(separator);
            }
            // Removing the last appended separator
            if (separator.length() > 0) {
                updatedSpanBuffer.setLength(updatedSpanBuffer.length() - separator.length());
            }
            return new MySpanData(log, log.getAttributes(), updatedSpanBuffer.toString());
        }
        return log;
    }

    //The following function extracts attributes from span name and replaces extracted parts with attribute names
    public SpanData processToAttributes(SpanData log) {
        if (toAttributeRulePatterns.isEmpty()) {
            return log;
        }
        String spanName = log.getName();
        // copy existing attributes.
        // According to Collector docs, The matched portion
        // in the span name is replaced by extracted attribute name. If the attributes exist
        // they will be overwritten. Need a way to optimize this.
        AttributesBuilder builder = log.getAttributes().toBuilder();
        for (int i = 0; i < groupNames.size(); i++) {
            spanName = applyRule(groupNames.get(i), toAttributeRulePatterns.get(i), spanName, builder);
        }
        return new MySpanData(log, builder.build(), spanName);

    }
}

