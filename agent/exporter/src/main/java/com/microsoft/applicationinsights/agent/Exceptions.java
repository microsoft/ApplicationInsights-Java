package com.microsoft.applicationinsights.agent;

import java.util.ArrayList;
import java.util.List;

import com.azure.monitor.opentelemetry.exporter.implementation.models.TelemetryExceptionDetails;

import static java.util.Collections.singletonList;

public class Exceptions {

    public static List<TelemetryExceptionDetails> minimalParse(String str) {
        TelemetryExceptionDetails details = new TelemetryExceptionDetails();
        int separator = -1;
        int length = str.length();
        int current;
        for (current = 0; current < length; current++) {
            char c = str.charAt(current);
            if (c == ':') {
                separator = current;
            } else if (c == '\r' || c == '\n') {
                break;
            }
        }
        // at the end of the loop, current will be end of the first line
        if (separator != -1) {
            details.setTypeName(str.substring(0, separator));
            details.setMessage(str.substring(separator + 2, current));
        } else {
            details.setTypeName(str.substring(0, current));
        }
        details.setStack(str);
        return singletonList(details);
    }

    // THIS IS UNFINISHED WORK
    // NOT SURE IF IT'S NEEDED
    // TESTING WITH minimalParse() first
    public static List<TelemetryExceptionDetails> fullParse(String str) {
        Parser parser = new Parser();
        for (String line : str.split("\r?\n")) {
            parser.process(line);
        }
        return parser.getDetails();
    }

    private Exceptions() {}

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
        }

        public List<TelemetryExceptionDetails> getDetails() {
            if (current != null) {
                list.add(current);
            }
            return list;
        }
    }
}
