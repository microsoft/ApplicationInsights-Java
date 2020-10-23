package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.InstrumentationSettings.ProcessorConfig;
import io.opentelemetry.common.Attributes;
import io.opentelemetry.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.checkerframework.checker.nullness.qual.Nullable;


public class SpanProcessor extends AgentProcessor {
    private static final Pattern capturingGroupNames = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
    private final List<String> fromAttributes;
    private final List<String> toAttributeRules;
    private final List<Pattern> toAttributeRulePatterns;
    private final List<List<String>> groupNames;
    private final String separator;

    public SpanProcessor(@Nullable IncludeExclude include,
                         @Nullable IncludeExclude exclude,
                         boolean isValidConfig,
                         List<String> fromAttributes,
                         List<String> toAttributeRules,
                         List<Pattern> toAttributeRulePatterns,
                         List<List<String>> groupNames,
                         String separator) {
        super(include, exclude, isValidConfig);
        this.fromAttributes = fromAttributes;
        this.toAttributeRules = toAttributeRules;
        this.toAttributeRulePatterns = toAttributeRulePatterns;
        this.groupNames = groupNames;
        this.separator = separator;
    }

    public static SpanProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<String> fromAttributes = new ArrayList<>();
        if (config.name.fromAttributes != null) {
            fromAttributes.addAll(config.name.fromAttributes);
        }
        List<String> toAttributeRules = new ArrayList<>();
        if (config.name.toAttributes != null) {
            toAttributeRules.addAll(config.name.toAttributes.rules);
        }
        List<Pattern> toAttributeRulePatterns = new ArrayList<>();
        if (config.name.toAttributes != null) {
            for (String rule : config.name.toAttributes.rules) {
                toAttributeRulePatterns.add(Pattern.compile(rule));
            }
        }
        List<List<String>> groupNames = getGroupNames(toAttributeRules);
        String separator = config.name.separator != null ? config.name.separator : "";
        return new SpanProcessor(normalizedInclude, normalizedExclude, true,
                fromAttributes, toAttributeRules, toAttributeRulePatterns, groupNames, separator);
    }

    private static List<List<String>> getGroupNames(List<String> toAttributeRules) {
        List<List<String>> groupNames = new ArrayList<>();
        for (String rule : toAttributeRules) {
            List<String> subGroupList = new ArrayList<>();
            Matcher matcher = capturingGroupNames.matcher(rule);
            while (matcher.find()) {
                subGroupList.add(matcher.group(1));
            }
            groupNames.add(subGroupList);
        }
        return groupNames;
    }

    //fromAttributes represents the attribute keys to pull the values from to generate the new span name.
    public SpanData processFromAttributes(SpanData span) {
        if (spanHasAllFromAttributeKeys(span)) {
            StringBuffer updatedSpanBuffer = new StringBuffer();
            ReadableAttributes existingSpanAttributes = span.getAttributes();
            for (String attributeKey : fromAttributes) {
                updatedSpanBuffer.append(existingSpanAttributes.get(attributeKey));
                updatedSpanBuffer.append(separator);
            }
            // Removing the last appended separator
            if (separator.length() > 0) {
                updatedSpanBuffer.setLength(updatedSpanBuffer.length() - 1);
            }
            return new MySpanData(span, span.getAttributes(), new String(updatedSpanBuffer));
        }
        return span;
    }

    private boolean spanHasAllFromAttributeKeys(SpanData span) {
        if (fromAttributes.isEmpty()) return false;
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        for (String attributeKey : fromAttributes) {
            if (existingSpanAttributes.get(attributeKey) == null) return false;
        }
        return true;
    }

    public SpanData processToAttributes(SpanData span) {
        if (toAttributeRules.isEmpty()) {
            return span;
        }
        String existingSpanName = span.getName();
        String updatedSpanName = span.getName();
        final Attributes.Builder builder = Attributes.newBuilder();
        for (int i = 0; i < groupNames.size(); i++) {
            updatedSpanName = applyRule(groupNames.get(i), toAttributeRulePatterns.get(i), span, updatedSpanName, builder);
        }
        if (existingSpanName.equals(updatedSpanName)) {
            return span;
        }
        //copy existing attributes
        span.getAttributes().forEach(builder::setAttribute);
        return new MySpanData(span, builder.build(), updatedSpanName);

    }

    private String applyRule(List<String> groupNamesList, Pattern pattern,
                             SpanData span, String spanName, Attributes.Builder builder) {
        if (groupNamesList.isEmpty()) return spanName;
        Matcher matcher = pattern.matcher(spanName);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(spanName, lastEnd, matcher.start());
            int innerLastEnd = matcher.start();
            for (int i = 1; i <= groupNames.size(); i++) {
                sb.append(spanName, innerLastEnd, matcher.start(i));
                sb.append("{");
                sb.append(groupNames.get(i - 1));
                // add attribute key=groupNames.get(i-1), value=matcher.group(i)
                builder.setAttribute(groupNamesList.get(i - 1), matcher.group(i));
                sb.append("}");
                innerLastEnd = matcher.end(i);
            }
            sb.append(spanName, innerLastEnd, matcher.end());
            lastEnd = matcher.end();
        }
        sb.append(spanName, lastEnd, spanName.length());

        return sb.toString();
    }
}

