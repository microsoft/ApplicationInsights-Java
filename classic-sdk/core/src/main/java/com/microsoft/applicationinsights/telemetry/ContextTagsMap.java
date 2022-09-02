// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.extensibility.context.ContextTagKeys;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nullable;

/** This ensures the values for certain tags do not exceed their limits. */
class ContextTagsMap implements ConcurrentMap<String, String> {

  private static final Map<String, Integer> tagSizeLimits = tagSizeLimits();

  private final ConcurrentMap<String, String> tags = new ConcurrentHashMap<>();

  private static String truncate(String value, int maxLength) {
    if (value != null && value.length() > maxLength) {
      value = value.substring(0, maxLength);
    }
    return value;
  }

  private static String sanitizeValue(String key, String value) {
    if (value != null) {
      value = value.trim();
    }
    if (tagSizeLimits.containsKey(key)) {
      value = truncate(value, tagSizeLimits.get(key));
    }
    return value;
  }

  @Override
  @Nullable
  public String putIfAbsent(String key, String value) {
    return tags.putIfAbsent(key, sanitizeValue(key, value));
  }

  @Override
  public boolean replace(String key, String oldValue, String newValue) {
    return tags.replace(key, oldValue, sanitizeValue(key, newValue));
  }

  @Override
  @Nullable
  public String replace(String key, String value) {
    return tags.replace(key, sanitizeValue(key, value));
  }

  @Override
  public int size() {
    return tags.size();
  }

  @Override
  public boolean isEmpty() {
    return tags.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return tags.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return tags.containsValue(value);
  }

  @Override
  @Nullable
  public String get(Object key) {
    return tags.get(key);
  }

  @Override
  @Nullable
  public String put(String key, String value) {
    return tags.put(key, sanitizeValue(key, value));
  }

  @Override
  @Nullable
  public String remove(Object key) {
    return tags.remove(key);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return tags.remove(key, value);
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> m) {
    Map<String, String> sanitized = new HashMap<>();
    for (Entry<? extends String, ? extends String> entry : m.entrySet()) {
      sanitized.put(entry.getKey(), sanitizeValue(entry.getKey(), entry.getValue()));
    }
    tags.putAll(sanitized);
  }

  @Override
  public void clear() {
    tags.clear();
  }

  @Override
  public Set<String> keySet() {
    return tags.keySet();
  }

  @Override
  public Collection<String> values() {
    return tags.values();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return tags.entrySet();
  }

  @Override
  public boolean equals(Object o) {
    return tags.equals(o);
  }

  @Override
  public int hashCode() {
    return tags.hashCode();
  }

  private static Map<String, Integer> tagSizeLimits() {
    Map<String, Integer> limits = new HashMap<>();
    limits.put(ContextTagKeys.getKeys().getApplicationVersion(), 1024);
    limits.put(ContextTagKeys.getKeys().getDeviceId(), 1024);
    limits.put(ContextTagKeys.getKeys().getDeviceModel(), 256);
    limits.put(ContextTagKeys.getKeys().getDeviceOEMName(), 256);
    limits.put(ContextTagKeys.getKeys().getDeviceOSVersion(), 256);
    limits.put(ContextTagKeys.getKeys().getDeviceType(), 64);
    limits.put(ContextTagKeys.getKeys().getLocationIP(), 45);
    limits.put(ContextTagKeys.getKeys().getOperationId(), 128);
    limits.put(ContextTagKeys.getKeys().getOperationName(), 1024);
    limits.put(ContextTagKeys.getKeys().getOperationParentId(), 128);
    limits.put(ContextTagKeys.getKeys().getSyntheticSource(), 1024);
    limits.put(ContextTagKeys.getKeys().getSessionId(), 64);
    limits.put(ContextTagKeys.getKeys().getUserId(), 128);
    limits.put(ContextTagKeys.getKeys().getUserAccountId(), 1024);
    limits.put(ContextTagKeys.getKeys().getUserAuthUserId(), 1024);
    limits.put(ContextTagKeys.getKeys().getCloudRole(), 256);
    limits.put(ContextTagKeys.getKeys().getCloudRoleInstance(), 256);
    limits.put(ContextTagKeys.getKeys().getOperationCorrelationVector(), 64);
    return limits;
  }
}
