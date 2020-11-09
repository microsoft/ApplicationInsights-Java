package com.microsoft.applicationinsights.agent.internal.processors;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.applicationinsights.agent.bootstrap.configuration.Configuration.ProcessorConfig;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.ReadableAttributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.checkerframework.checker.nullness.qual.Nullable;


public class SpanProcessor extends AgentProcessor {
    private static final Pattern capturingGroupNames = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");
    private final List<AttributeKey<?>> fromAttributes;
    private final List<Pattern> toAttributeRulePatterns;
    private final List<List<String>> groupNames;
    private final String separator;

    public SpanProcessor(@Nullable IncludeExclude include,
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

    public static SpanProcessor create(ProcessorConfig config) {
        IncludeExclude normalizedInclude = config.include != null ? getNormalizedIncludeExclude(config.include) : null;
        IncludeExclude normalizedExclude = config.exclude != null ? getNormalizedIncludeExclude(config.exclude) : null;
        List<AttributeKey<?>> fromAttributes = new ArrayList<>();
        if (config.name.fromAttributes != null) {
            for (String attribute : config.name.fromAttributes) {
                fromAttributes.add(AttributeKey.stringKey(attribute));
            }
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
        return new SpanProcessor(normalizedInclude, normalizedExclude,
                fromAttributes, toAttributeRulePatterns, groupNames, separator);
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
            for (AttributeKey<?> attributeKey : fromAttributes) {
                updatedSpanBuffer.append(existingSpanAttributes.get(attributeKey));
                updatedSpanBuffer.append(separator);
            }
            // Removing the last appended separator
            if (separator.length() > 0) {
                updatedSpanBuffer.setLength(updatedSpanBuffer.length() - separator.length());
            }
            return new MySpanData(span, span.getAttributes(), new String(updatedSpanBuffer));
        }
        return span;
    }

    private boolean spanHasAllFromAttributeKeys(SpanData span) {
        if (fromAttributes.isEmpty()) return false;
        ReadableAttributes existingSpanAttributes = span.getAttributes();
        for (AttributeKey<?> attributeKey : fromAttributes) {
            if (existingSpanAttributes.get(attributeKey) == null) return false;
        }
        return true;
    }

    //The following function extracts attributes from span name and replaces extracted parts with attribute names
    public SpanData processToAttributes(SpanData span) {
        if (toAttributeRulePatterns.isEmpty()) {
            return span;
        }

        String spanName = span.getName();
        final Attributes.Builder builder = Attributes.builder();
        // copy existing attributes.
        // According to Collector docs, The matched portion
        // in the span name is replaced by extracted attribute name. If the attributes exist
        // they will be overwritten. Need a way to optimize this.
        span.getAttributes().forEach(builder::put);
        for (int i = 0; i < groupNames.size(); i++) {
            spanName = applyRule(groupNames.get(i), toAttributeRulePatterns.get(i), span, spanName, builder);
        }
        return new MySpanData(span, builder.build(), spanName);

    }

    private String applyRule(List<String> groupNamesList, Pattern pattern,
                             SpanData span, String spanName, Attributes.Builder builder) {
        if (groupNamesList.isEmpty()) return spanName;
        Matcher matcher = pattern.matcher(spanName);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        // As of now we are considering only first match.
        if (matcher.find()) {
            sb.append(spanName, lastEnd, matcher.start());
            int innerLastEnd = matcher.start();
            for (int i = 1; i <= groupNamesList.size(); i++) {
                sb.append(spanName, innerLastEnd, matcher.start(i));
                sb.append("{");
                sb.append(groupNamesList.get(i - 1));
                // add attribute key=groupNames.get(i-1), value=matcher.group(i)
                builder.put(groupNamesList.get(i - 1), matcher.group(i));
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

