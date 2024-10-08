// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.diagnostics.collection.jvm;

import com.azure.json.JsonSerializable;
import com.azure.json.JsonWriter;
import com.microsoft.applicationinsights.diagnostics.collection.libos.process.ProcessInfo;
import java.io.IOException;
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
public class ProcessData implements ProcessInfo, JsonSerializable<ProcessData> {

  private String name;
  private int pid;
  private String uid = UUID.randomUUID().toString();

  @Nullable private Map<String, String> metaData;

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

  public ProcessData() {}

  public static String sanetiseArg(String name) {

    for (String pattern : SENSITIVE_ARGS_PATTERNS) {
      name = name.replaceAll(pattern, "");
    }

    return name;
  }

  public ProcessData(String name, int pid) {
    setName(name).setPid(pid);
  }

  public ProcessData(String name, int pid, Map<String, String> metaData) {
    setName(name).setPid(pid).setMetaData(metaData);
  }

  public ProcessData(String name, int pid, String uid) {
    setName(name).setPid(pid).setUid(uid);
  }

  public ProcessData(String name) {
    setName(name).setPid(-1);
  }

  public ProcessData(ProcessInfo clone) {
    setName(clone == null ? "Unknown" : clone.getName())
        .setPid(clone == null ? -1 : clone.getPid())
        .setUid(clone == null ? UUID.randomUUID().toString() : clone.getUid())
        .setMetaData(clone == null ? null : clone.getMetaData());
  }

  @Override
  public String getName() {
    return name;
  }

  public ProcessData setName(String name) {
    this.name = sanetiseArg(name);
    return this;
  }

  @Override
  public int getPid() {
    return pid;
  }

  public ProcessData setPid(int pid) {
    this.pid = pid;
    return this;
  }

  @Override
  public String getUid() {
    return uid;
  }

  public ProcessData setUid(String uid) {
    this.uid = uid;
    return this;
  }

  @Override
  @Nullable
  public Map<String, String> getMetaData() {
    return metaData;
  }

  public ProcessData setMetaData(Map<String, String> metaData) {
    this.metaData = metaData != null ? Collections.unmodifiableMap(metaData) : null;
    return this;
  }

  @Override
  public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
    jsonWriter.writeStartObject();
    jsonWriter.writeStringField("name", name);
    jsonWriter.writeIntField("pid", pid);
    jsonWriter.writeStringField("uid", uid);
    jsonWriter.writeMapField("metaData", metaData, JsonWriter::writeString);
    jsonWriter.writeEndObject();
    return jsonWriter;
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
}
