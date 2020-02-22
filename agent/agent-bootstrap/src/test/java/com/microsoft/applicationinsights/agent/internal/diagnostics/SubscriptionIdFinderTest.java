package com.microsoft.applicationinsights.agent.internal.diagnostics;

import org.junit.*;
import org.junit.contrib.java.lang.system.*;

import static org.junit.Assert.*;

public class SubscriptionIdFinderTest {
    @Rule
    public EnvironmentVariables envVars = new EnvironmentVariables();

    private SubscriptionIdFinder subIdFinder;

    @Before
    public void setup() {
        subIdFinder = new SubscriptionIdFinder();
    }

    @After
    public void tearDown() {
        subIdFinder = null;
    }

    @Test
    public void happyPathWithValidSubscriptionIdInsideWebsiteOwnerNameVar() {
        envVars.set(SubscriptionIdFinder.WEBSITE_OWNER_NAME_ENV_VAR, "sub-id-123+NOT_SUB_ID");
        final String value = subIdFinder.getValue();
        assertEquals("sub-id-123", value);
    }

    @Test
    public void useUnknownWhenEnvVarIsUnset() {
        assertEquals("unknown", subIdFinder.getValue());
    }

    @Test
    public void useUnknownIfEnvVarHasUnexpectedFormat() {
        envVars.set(SubscriptionIdFinder.WEBSITE_OWNER_NAME_ENV_VAR, "NOT_VALID");
        assertEquals("unknown", subIdFinder.getValue());
    }
}
