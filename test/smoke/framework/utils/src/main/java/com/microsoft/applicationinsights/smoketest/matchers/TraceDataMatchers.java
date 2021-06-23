package com.microsoft.applicationinsights.smoketest.matchers;

import com.microsoft.applicationinsights.internal.schemav2.MessageData;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class TraceDataMatchers {
    public static Matcher<MessageData> hasMessage(String expectedMessage) {
        return new FeatureMatcher<MessageData, String>(equalTo(expectedMessage), "MessageData with message", "message") {
            @Override
            protected String featureValueOf(MessageData actual) {
                return actual.getMessage();
            }
        };
    }

    public static Matcher<MessageData> hasSeverityLevel(SeverityLevel severityLevel) {
        return new FeatureMatcher<MessageData, SeverityLevel>(equalTo(severityLevel), "MessageData with SeverityLevel", "SeverityLevel") {
            @Override
            protected SeverityLevel featureValueOf(MessageData actual) {
                return actual.getSeverityLevel();
            }
        };
    }

    public static Matcher<MessageData> hasProperty(String key, String value) {
        return new FeatureMatcher<MessageData, Map<String, String>>(hasEntry(key, value), "MessageData with property", "property") {
            @Override
            protected Map<String, String> featureValueOf(MessageData actual) {
                return actual.getProperties();
            }
        };
    }

    private TraceDataMatchers() {}
}
