// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.cgroups;

import com.microsoft.applicationinsights.diagnostics.collection.libos.BigIncrementalCounter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux.TwoStepProcReader;
import java.io.File;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CGroupStatReader extends TwoStepProcReader {
  private final BigIncrementalCounter user = new BigIncrementalCounter();
  private final BigIncrementalCounter system = new BigIncrementalCounter();

  public CGroupStatReader() {
    super(new File("/sys/fs/cgroup/cpu,cpuacct/cpuacct.stat"), true);
  }

  @Override
  protected void parseLine(String line) {
    /*
     Example contents:
     ```
       user 877968
       system 127178
     ```
    */
    String[] tokens = line.split(" ");

    if (tokens.length == 2) {
      if ("user".equals(tokens[0])) {
        user.newValue(Long.parseLong(tokens[1]));
      } else if ("system".equals(tokens[0])) {
        system.newValue(Long.parseLong(tokens[1]));
      }
    }
  }

  public BigIncrementalCounter getUser() {
    return user;
  }

  public BigIncrementalCounter getSystem() {
    return system;
  }
}
