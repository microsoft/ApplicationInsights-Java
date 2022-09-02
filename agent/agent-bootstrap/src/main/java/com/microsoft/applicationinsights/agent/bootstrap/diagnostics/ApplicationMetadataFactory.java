// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap.diagnostics;

import java.util.Arrays;
import java.util.Iterator;

public class ApplicationMetadataFactory implements Iterable<DiagnosticsValueFinder> {

  private final DiagnosticsValueFinder[] finders =
      new DiagnosticsValueFinder[] {
        new AgentExtensionVersionFinder(), // 0
        new InstrumentationKeyFinder(), // 1
        new MachineNameFinder(), // 2
        new PidFinder(), // 3
        new SiteNameFinder(), // 4
        new SubscriptionIdFinder(), // 5
        new SdkVersionFinder(), // 6
      };

  ApplicationMetadataFactory() {}

  public DiagnosticsValueFinder getExtensionVersion() {
    return finders[0];
  }

  public DiagnosticsValueFinder getInstrumentationKey() {
    return finders[1];
  }

  public DiagnosticsValueFinder getMachineName() {
    return finders[2];
  }

  public DiagnosticsValueFinder getPid() {
    return finders[3];
  }

  public DiagnosticsValueFinder getSiteName() {
    return finders[4];
  }

  public DiagnosticsValueFinder getSubscriptionId() {
    return finders[5];
  }

  public DiagnosticsValueFinder getSdkVersion() {
    return finders[6];
  }

  @Override
  public Iterator<DiagnosticsValueFinder> iterator() {
    return Arrays.asList(finders).iterator();
  }
}
