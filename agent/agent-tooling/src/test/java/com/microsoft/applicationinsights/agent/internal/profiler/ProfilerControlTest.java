// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.alerting.alert.AlertBreach;
import com.microsoft.applicationinsights.alerting.config.AlertMetricType;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProfilerControlTest {

  private static final String OBJECT_NAME = "com.microsoft:type=AI-alert,name=ProfilerControl";

  @AfterEach
  void cleanup() throws Exception {
    // Unregister MBean if it was registered during the test
    MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName(OBJECT_NAME);
    if (beanServer.isRegistered(objectName)) {
      beanServer.unregisterMBean(objectName);
    }
  }

  @Test
  void triggerProfileWithDefaultDuration() {
    AtomicReference<AlertBreach> received = new AtomicReference<>();
    Consumer<AlertBreach> handler = received::set;

    assertThat(result).startsWith("Profile trigger requested");

    AlertBreach breach = received.get();
    assertThat(breach).isNotNull();
    assertThat(breach.getType()).isEqualTo(AlertMetricType.MANUAL);
    assertThat(breach.getAlertConfiguration().getProfileDurationSeconds()).isEqualTo(120);
    assertThat(breach.getProfileId()).isNotNull();
  }

  @Test
  void registerCreatesAccessibleMBean() throws Exception {
    AtomicReference<AlertBreach> received = new AtomicReference<>();
    Consumer<AlertBreach> handler = received::set;

    ProfilerControl.register(handler);

    MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName(OBJECT_NAME);

    assertThat(beanServer.isRegistered(objectName)).isTrue();

    // Invoke triggerProfile via JMX
    Object result = beanServer.invoke(objectName, "triggerProfile", null, null);

    assertThat(result).isInstanceOf(String.class);
    assertThat((String) result).contains("duration=120s");
    assertThat(received.get()).isNotNull();
    assertThat(received.get().getType()).isEqualTo(AlertMetricType.MANUAL);
  }

  @Test
  void registerWithCustomDurationViaMBean() throws Exception {
    AtomicReference<AlertBreach> received = new AtomicReference<>();
    Consumer<AlertBreach> handler = received::set;

    ProfilerControl.register(handler);

    MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName(OBJECT_NAME);

    Object result =
        beanServer.invoke(objectName, "triggerProfile", new Object[] {45}, new String[] {"int"});

    assertThat(result).isInstanceOf(String.class);
    assertThat((String) result).contains("duration=45s");
    assertThat(received.get()).isNotNull();
    assertThat(received.get().getAlertConfiguration().getProfileDurationSeconds()).isEqualTo(45);
  }

  @Test
  void registerDoesNotThrowOnDoubleRegistration() {
    Consumer<AlertBreach> handler = breach -> {};

    ProfilerControl.register(handler);
    // Should not throw
    ProfilerControl.register(handler);
  }
}
