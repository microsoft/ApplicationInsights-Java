package com.microsoft.applicationinsights.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionDetails;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import static java.util.Collections.singletonList;

public class Exceptions {

    private static final Splitter lineSplitter = Splitter.on(CharMatcher.anyOf("\r\n")).omitEmptyStrings();

    public static List<TelemetryExceptionDetails> minimalParse(String str) {
        TelemetryExceptionDetails details = new TelemetryExceptionDetails();
        String line = lineSplitter.split(str).iterator().next();
        int index = line.indexOf(": ");
        if (index != -1) {
            details.setTypeName(line.substring(0, index));
            details.setMessage(line.substring(index + 2));
        } else {
            details.setTypeName(line);
        }
        details.setStack(str);
        return singletonList(details);
    }

    // THIS IS UNFINISHED WORK
    // NOT SURE IF IT'S NEEDED
    // TESTING WITH minimalParse() first
    public static List<TelemetryExceptionDetails> fullParse(String str) {
        Parser parser = new Parser();
        for (String line : lineSplitter.split(str)) {
            parser.process(line);
        }
        return parser.getDetails();
    }

    static class Parser {

        private TelemetryExceptionDetails current;
        private final List<TelemetryExceptionDetails> list = new ArrayList<>();

        void process(String line) {
            if (line.charAt(0) != '\t') {
                if (current != null) {
                    list.add(current);
                }
                if (line.startsWith("Caused by: ")) {
                    line = line.substring("Caused by: ".length());
                }
                current = new TelemetryExceptionDetails();
                int index = line.indexOf(": ");
                if (index != -1) {
                    current.setTypeName(line.substring(0, index));
                    current.setMessage(line.substring(index + 2));
                } else {
                    current.setTypeName(line);
                }
            }
            System.out.println(line);
        }

        public List<TelemetryExceptionDetails> getDetails() {
            if (current != null) {
                list.add(current);
            }
            return list;
        }
    }
}
