package com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ExtractAttribute;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorAction;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration.ProcessorActionJson;
import com.microsoft.applicationinsights.customExceptions.FriendlyException;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.ToJson;

public class ProcessorActionAdaptor {
    protected static final Pattern capturingGroupNames = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

    public static List<String> getGroupNames(String regex) {
        List<String> groupNames = new ArrayList<>();
        Matcher matcher = capturingGroupNames.matcher(regex);
        while (matcher.find()) {
            groupNames.add(matcher.group(1));
        }
        return groupNames;
    }

    @FromJson
    ProcessorAction fromJson(ProcessorActionJson processorActionJson) {
        try {
            ProcessorAction processorAction = new ProcessorAction();
            processorAction.key = processorActionJson.key;
            processorAction.action = processorActionJson.action;
            processorAction.fromAttribute = processorActionJson.fromAttribute;
            processorAction.value = processorActionJson.value;
            if (processorActionJson.pattern == null) {
                return processorAction; // If pattern not present, no further processing required
            }
            String pattern = processorActionJson.pattern;
            Pattern regexPattern = Pattern.compile(pattern);
            List<String> groupNames = getGroupNames(pattern);
            processorAction.extractAttribute = new ExtractAttribute(regexPattern, groupNames);
            return processorAction;
        } catch (PatternSyntaxException e) {
            throw new FriendlyException("Telemetry processor configuration does not have valid regex:" + processorActionJson.pattern,
                    "Please provide a valid regex in the telemetry processors configuration. " +
                            "Learn more about telemetry processors here: https://go.microsoft.com/fwlink/?linkid=2151557", e);
        }
    }

    @ToJson
    ProcessorActionJson toJson(ProcessorAction processorAction) {
        throw new UnsupportedOperationException();
    }
}
