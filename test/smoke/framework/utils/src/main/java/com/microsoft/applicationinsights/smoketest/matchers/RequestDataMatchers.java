package com.microsoft.applicationinsights.smoketest.matchers;

import com.microsoft.applicationinsights.internal.schemav2.RequestData;
import com.microsoft.applicationinsights.telemetry.Duration;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.*;

public class RequestDataMatchers {
    public static Matcher<RequestData> hasName(String name) {
        return new FeatureMatcher<RequestData, String>(equalTo(name), "RequestData with name", "name") {
            @Override
            protected String featureValueOf(RequestData actual) {
                return actual.getName();
            }
        };
    }

    public static Matcher<RequestData> hasDuration(Duration duration) {
        return new FeatureMatcher<RequestData, Duration>(equalTo(duration), "RequestData with duration", "duration") {
            @Override
            protected Duration featureValueOf(RequestData actual) {
                return actual.getDuration();
            }
        };
    }

    public static Matcher<RequestData> hasSuccess(boolean success) {
        return new FeatureMatcher<RequestData, Boolean>(equalTo(success), "RequestData with success", "success") {
            @Override
            protected Boolean featureValueOf(RequestData actual) {
                return actual.getSuccess();
            }
        };
    }

    public static Matcher<RequestData> hasResponseCode(String responseCode) {
        return new FeatureMatcher<RequestData, String>(equalTo(responseCode), "RequestData with response code", "response code") {
            @Override
            protected String featureValueOf(RequestData actual) {
                return actual.getResponseCode();
            }
        };
    }

    public static Matcher<RequestData> hasUrl(String url) {
        return new FeatureMatcher<RequestData, String>(equalTo(url), "ReqeustData with url", "url") {
            @Override
            protected String featureValueOf(RequestData actual) {
                return actual.getUrl();
            }
        };
    }

    private RequestDataMatchers() {}
}
