package com.microsoft.applicationinsights.smoketest.matchers;

import com.microsoft.applicationinsights.internal.schemav2.PageViewData;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class PageViewDataMatchers {
    public static Matcher<PageViewData> hasName(String name) {
        return new FeatureMatcher<PageViewData, String>(equalTo(name), "PageViewData with name", "name") {
            @Override
            protected String featureValueOf(PageViewData actual) {
                return actual.getName();
            }
        };
    }

    public static Matcher<PageViewData> hasDuration(Duration duration) {
        return new FeatureMatcher<PageViewData, Duration>(equalTo(duration), "PageViewData with duration", "duration") {
            @Override
            protected Duration featureValueOf(PageViewData actual) {
                return actual.getDuration();
            }
        };
    }

    public static Matcher<PageViewData> hasProperty(String key, String value) {
        return new FeatureMatcher<PageViewData, Map<String, String>>(hasEntry(key, value), "PageViewData with property", "property") {
            @Override
            protected Map<String, String> featureValueOf(PageViewData actual) {
                return actual.getProperties();
            }
        };
    }
}
