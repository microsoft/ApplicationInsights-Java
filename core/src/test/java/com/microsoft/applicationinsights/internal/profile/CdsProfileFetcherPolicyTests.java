package com.microsoft.applicationinsights.internal.profile;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(JUnit4.class)
public class CdsProfileFetcherPolicyTests {

    @After
    public void clear() {
        CdsProfileFetcherPolicy.getInstance().resetConfiguration();
    }

    @Test
    public void testInstanceOfCdsProfileFetcherIsCreated() {
        CdsProfileFetcherPolicy.getInstance();
    }

    @Test
    public void callingGetInstanceMultipleTimesReturnsCachedInstance() {
        CdsProfileFetcherPolicy policy = CdsProfileFetcherPolicy.getInstance();
        CdsProfileFetcherPolicy policy1 = CdsProfileFetcherPolicy.getInstance();
        assertThat(policy, equalTo(policy1));
    }

    @Test
    public void defaultConfigurationIsSetWhenNewInstanceIsCreated() {
        CdsProfileFetcherPolicy policy = CdsProfileFetcherPolicy.getInstance();
        assertThat(policy.getMaxInstantRetries(), equalTo(3));
        assertThat(policy.getCachePurgePeriodInMinutes(), equalTo(240L));
    }

    @Test
    public void defaultConfigurationCanBeOverriden() {
        CdsProfileFetcherPolicy policy = CdsProfileFetcherPolicy.getInstance();
        assertThat(policy.getMaxInstantRetries(), equalTo(3));
        assertThat(policy.getCachePurgePeriodInMinutes(), equalTo(240L));

        policy.setCachePurgePeriodInMinutes(1);
        policy.setMaxInstantRetries(1);
        assertThat(policy.getMaxInstantRetries(), equalTo(1));
        assertThat(policy.getCachePurgePeriodInMinutes(), equalTo(1L));
    }
}
