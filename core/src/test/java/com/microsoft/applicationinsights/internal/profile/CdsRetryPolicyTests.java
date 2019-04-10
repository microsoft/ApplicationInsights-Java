package com.microsoft.applicationinsights.internal.profile;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(JUnit4.class)
public class CdsRetryPolicyTests {

    @After
    public void clear() {
        CdsRetryPolicy.INSTANCE.resetConfiguration();
    }

    @Test
    public void testInstanceOfCdsProfileFetcherIsCreated() {
        assertThat(CdsRetryPolicy.INSTANCE, is(notNullValue()));
    }

    @Test
    public void defaultConfigurationIsSetWhenNewInstanceIsCreated() {
        CdsRetryPolicy policy = CdsRetryPolicy.INSTANCE;
        assertThat(policy.getMaxInstantRetries(), equalTo(3));
        assertThat(policy.getResetPeriodInMinutes(), equalTo(240L));
    }

    @Test
    public void defaultConfigurationCanBeOverridden() {
        CdsRetryPolicy policy = CdsRetryPolicy.INSTANCE;
        assertThat(policy.getMaxInstantRetries(), equalTo(3));
        assertThat(policy.getResetPeriodInMinutes(), equalTo(240L));

        policy.setResetPeriodInMinutes(1);
        policy.setMaxInstantRetries(1);
        assertThat(policy.getMaxInstantRetries(), equalTo(1));
        assertThat(policy.getResetPeriodInMinutes(), equalTo(1L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetMaxInstantRetriesLessThan1() {
        CdsRetryPolicy.INSTANCE.setMaxInstantRetries(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotSetResetPeriodLessThan1Minute() {
        CdsRetryPolicy.INSTANCE.setResetPeriodInMinutes(0);
    }
}

