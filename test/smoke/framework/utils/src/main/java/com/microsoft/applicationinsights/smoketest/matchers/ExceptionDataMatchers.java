package com.microsoft.applicationinsights.smoketest.matchers;

import com.microsoft.applicationinsights.internal.schemav2.ExceptionData;
import com.microsoft.applicationinsights.internal.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.internal.schemav2.SeverityLevel;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class ExceptionDataMatchers {

    public static Matcher<ExceptionData> hasSeverityLevel(SeverityLevel level) {
        return new FeatureMatcher<ExceptionData, SeverityLevel>(equalTo(level), "seveirity level to be", "severity level") {

            @Override
            protected SeverityLevel featureValueOf(ExceptionData actual) {
                return actual.getSeverityLevel();
            }
        };
    }

    public static Matcher<ExceptionData> hasProperty(String key, String value) {
        return new FeatureMatcher<ExceptionData, Map<String, String>>(hasEntry(key, value), "ExceptionData with property", "") {

            @Override
            protected Map<String, String> featureValueOf(ExceptionData actual) {
                return actual.getProperties();
            }
        };
    }

    public static Matcher<ExceptionData> hasMeasurement(String key, Double value) {
        return new FeatureMatcher<ExceptionData, Map<String, Double>>(hasEntry(key, value), "ExceptionData with measurement", "") {

            @Override
            protected Map<String, Double> featureValueOf(ExceptionData actual) {
                return actual.getMeasurements();
            }
        };
    }

    public static Matcher<ExceptionData> hasException(Matcher<ExceptionDetails> exception) {
        return new FeatureMatcher<ExceptionData, List<ExceptionDetails>>(hasItem(exception), "ExceptionData with exception", "") {

            @Override
            protected List<ExceptionDetails> featureValueOf(ExceptionData actual) {
                return actual.getExceptions();
            }
        };
    }

    public static class ExceptionDetailsMatchers {
        public static Matcher<ExceptionDetails> withMessage(String message) {
            return new FeatureMatcher<ExceptionDetails, String>(equalTo(message), "ExceptionDetails with message", "message") {

                @Override
                protected String featureValueOf(ExceptionDetails actual) {
                    return actual.getMessage();
                }
            };
        }

        private ExceptionDetailsMatchers() {}
    }

    private ExceptionDataMatchers() {}
}
