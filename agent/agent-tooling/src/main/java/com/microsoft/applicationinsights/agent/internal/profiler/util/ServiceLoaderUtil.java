// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.util;

import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class ServiceLoaderUtil {

  public static <T> T findServiceLoader(Class<T> clazz) {
    return findServiceLoader(clazz, false);
  }

  public static <T> T findServiceLoader(Class<T> clazz, boolean includeExtensionClassPath) {
    List<T> services = findAllServiceLoaders(clazz, includeExtensionClassPath);
    if (!services.isEmpty()) {
      return services.get(0);
    }
    return null;
  }

  public static <T> List<T> findAllServiceLoaders(
      Class<T> clazz, boolean includeExtensionClassPath) {
    List<T> serviceLoaders = new ArrayList<T>();

    ServiceLoader<T> factory = ServiceLoader.load(clazz);
    Iterator<T> iterator = factory.iterator();
    iterator.forEachRemaining(serviceLoaders::add);

    if (includeExtensionClassPath) {
      ClassLoader extensionsClassLoader = AgentInitializer.getExtensionsClassLoader();
      if (extensionsClassLoader != null) {
        factory = ServiceLoader.load(clazz, extensionsClassLoader);
        iterator = factory.iterator();
        iterator.forEachRemaining(serviceLoaders::add);
      }
    }
    return serviceLoaders;
  }

  private ServiceLoaderUtil() {}
}
