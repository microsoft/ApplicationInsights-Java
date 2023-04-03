// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.jvm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Represents information about a running process. Also attempts to redact any sensitive arguments.
 */
public class ProcessData implements ProcessInfo {

  private final String name;
  private final int pid;
  private final String uid;

  @Nullable private final Map<String, String> metaData;

  private static final List<String> SENSITIVE_PROPERTIES_ARGS =
      Arrays.asList(
          "javax\\.net\\.ssl\\.trustStorePassword",
          "javax\\.net\\.ssl\\.keyStorePassword",
          "assword");

  private static final List<String> SENSITIVE_ARGS = Arrays.asList("\\-P", "\\-p");

  private static final List<String> SENSITIVE_ARGS_PATTERNS;

  private static final Comparator<ProcessInfo> COMPARATOR =
      Comparator.nullsFirst(
          Comparator.comparing(ProcessInfo::getName).thenComparing(ProcessInfo::getPid));

  static {
    String spaceOrNull = " " + (char) 0;
    String notSpaceOrNull = "[^" + spaceOrNull + "]*";
    String isSpaceOrNull = "[" + spaceOrNull + "]";
    String isSpaceOrNullOrEquals = "[" + spaceOrNull + "=]*";
    List<String> patterns = new ArrayList<>();

    for (String regex : SENSITIVE_ARGS) {
      patterns.add(isSpaceOrNull + regex + isSpaceOrNullOrEquals + notSpaceOrNull);
    }

    for (String regex : SENSITIVE_PROPERTIES_ARGS) {
      patterns.add(
          "\\-D" + notSpaceOrNull + regex + notSpaceOrNull + "=" + notSpaceOrNull + isSpaceOrNull);
    }

    SENSITIVE_ARGS_PATTERNS = Collections.unmodifiableList(patterns);
  }

  public static String sanetiseArg(String name) {

    for (String pattern : SENSITIVE_ARGS_PATTERNS) {
      name = name.replaceAll(pattern, "");
    }

    return name;
  }

  public ProcessData(String name, int pid) {
    this(name, pid, UUID.randomUUID().toString());
  }

  public ProcessData(String name, int pid, Map<String, String> metaData) {
    this(name, pid, UUID.randomUUID().toString(), metaData);
  }

  public ProcessData(String name, int pid, String uid) {
    this(name, pid, uid, null);
  }

  public ProcessData(String name) {
    this(sanetiseArg(name), -1, UUID.randomUUID().toString(), null);
  }

  public ProcessData(ProcessInfo clone) {
    this(
        (clone == null ? "Unknown" : clone.getName()),
        (clone == null ? -1 : clone.getPid()),
        (clone == null ? UUID.randomUUID().toString() : clone.getUid()),
        (clone == null ? null : clone.getMetaData()));
  }

  @JsonCreator
  public ProcessData(
      @JsonProperty("name") String name,
      @JsonProperty("pid") int pid,
      @Nullable @JsonProperty("uid") String uid,
      @Nullable @JsonProperty("metaData") Map<String, String> metaData) {
    this.name = sanetiseArg(name);
    this.pid = pid;
    if (uid == null) {
      this.uid = UUID.randomUUID().toString();
    } else {
      this.uid = uid;
    }

    if (metaData != null) {
      this.metaData = Collections.unmodifiableMap(metaData);
    } else {
      this.metaData = null;
    }
  }

  // @ExistsForTesting
  protected static ProcessData create(String name, int pid) {
    return new ProcessData(name, pid);
  }

  protected static ProcessData create(String name, int pid, Map<String, String> metaData) {
    return new ProcessData(name, pid, metaData);
  }

  // @ExistsForTesting
  protected static ProcessData create(String name, int pid, @Nullable String uid) {
    return new ProcessData(name, pid, uid);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getPid() {
    return pid;
  }

  @Override
  public String getUid() {
    return uid;
  }

  @Override
  public int compareTo(ProcessInfo o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + pid;
    return result;
  }

  @Override
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ProcessData other = (ProcessData) obj;
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return pid == other.pid;
  }

  @Override
  @Nullable
  public Map<String, String> getMetaData() {
    return metaData;
  }
}
