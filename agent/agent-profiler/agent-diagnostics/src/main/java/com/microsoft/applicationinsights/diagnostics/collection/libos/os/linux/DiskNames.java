// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.libos.os.linux;

import java.util.regex.Pattern;

public class DiskNames {

  // e.g: md0, sda, hda, xvda1, nvme0n1
  private static final Pattern DISK_PATTERN =
      Pattern.compile("(md[0-9]+|sd[a-z]|hd[a-z]|xvd[a-z][0-9]|nvme[0-9]+n[0-9]+)");

  public static boolean matchesDiskName(String name) {
    return DISK_PATTERN.matcher(name).matches();
  }

  private DiskNames() {}
}
