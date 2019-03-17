package com.microsoft.applicationinsights.internal.agent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import com.microsoft.applicationinsights.agent.internal.coresync.impl.ImplementationsCoordinator;
import com.microsoft.applicationinsights.internal.agent.AgentConnector.RegistrationResult;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class AgentConnectorTest {

    @After
    public void clean() {
        AgentConnector.INSTANCE.unregisterAgent();
    }

    @Test
    public void universalAgentConnectorTest() {
        RegistrationResult registrationResult = AgentConnector.INSTANCE.universalAgentRegisterer();
        assertThat(registrationResult, is(notNullValue()));
        assertThat(ImplementationsCoordinator.INSTANCE.getNotificationHandler(), is(notNullValue()));
    }

    @Test
    public void cannotRegisterTwice() {
        RegistrationResult registrationResult = AgentConnector.INSTANCE.universalAgentRegisterer();
        RegistrationResult registrationResult1 = AgentConnector.INSTANCE.universalAgentRegisterer();
        assertThat(registrationResult1, is(sameInstance(registrationResult)));
    }

    @Test
    public void unRegisterAgentTest() {
        RegistrationResult registrationResult = AgentConnector.INSTANCE.universalAgentRegisterer();
        assertThat(registrationResult, is(notNullValue()));
        AgentConnector.INSTANCE.unregisterAgent();
        assertThat(AgentConnector.INSTANCE.registrationResult, is(nullValue()));
        assertThat(ImplementationsCoordinator.INSTANCE.getNotificationHandler(), is(nullValue()));
    }
}
