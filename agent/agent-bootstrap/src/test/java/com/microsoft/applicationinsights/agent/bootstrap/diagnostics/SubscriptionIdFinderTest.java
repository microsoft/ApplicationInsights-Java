package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SystemStubsExtension.class)
public class SubscriptionIdFinderTest {

    @SystemStub
    public EnvironmentVariables envVars = new EnvironmentVariables();

    private SubscriptionIdFinder subIdFinder;

    @BeforeEach
    public void setup() {
        subIdFinder = new SubscriptionIdFinder();
    }

    @AfterEach
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
