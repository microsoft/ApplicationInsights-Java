/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
