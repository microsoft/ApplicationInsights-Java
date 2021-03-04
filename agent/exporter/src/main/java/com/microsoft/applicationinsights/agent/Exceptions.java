package com.microsoft.applicationinsights.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exceptions {

    private static final Logger logger = LoggerFactory.getLogger(Exceptions.class);

    private static final Splitter lineSplitter = Splitter.on(CharMatcher.anyOf("\r\n")).omitEmptyStrings();

    public static List<ExceptionDetails> minimalParse(String str) {
        ExceptionDetails details = new ExceptionDetails();
        String line = lineSplitter.split(str).iterator().next();
        int index = line.indexOf(": ");
        if (index != -1) {
            details.setTypeName(line.substring(0, index));
            details.setMessage(line.substring(index + 2));
        } else {
            details.setTypeName(line);
        }
        details.setStack(str);
        return Arrays.asList(details);
    }

    // THIS IS UNFINISHED WORK
    // NOT SURE IF IT'S NEEDED
    // TESTING WITH minimalParse() first
    public static List<ExceptionDetails> fullParse(String str) {
        Parser parser = new Parser();
        for (String line : lineSplitter.split(str)) {
            parser.process(line);
        }
        return parser.getDetails();
    }

    static class Parser {

        private ExceptionDetails current;
        private final List<ExceptionDetails> list = new ArrayList<>();

        void process(String line) {
            if (line.charAt(0) != '\t') {
                if (current != null) {
                    list.add(current);
                }
                if (line.startsWith("Caused by: ")) {
                    line = line.substring("Caused by: ".length());
                }
                current = new ExceptionDetails();
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

        public List<ExceptionDetails> getDetails() {
            if (current != null) {
                list.add(current);
            }
            return list;
        }
    }

    static class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }
}
