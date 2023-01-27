// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.profiler.util;

import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import java.util.Iterator;
import java.util.ServiceLoader;

public class ServiceLoaderUtil {

  public static <T> T findServiceLoader(Class<T> clazz) {
    return findServiceLoader(clazz, false);
  }

  public static <T> T findServiceLoader(Class<T> clazz, boolean includeExtensionClassPath) {
    ServiceLoader<T> factory = ServiceLoader.load(clazz);
    Iterator<T> iterator = factory.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    if (includeExtensionClassPath) {
      ClassLoader extensionsClassLoader = AgentInitializer.getExtensionsClassLoader();
      if (extensionsClassLoader != null) {
        factory = ServiceLoader.load(clazz, extensionsClassLoader);
        iterator = factory.iterator();
        if (iterator.hasNext()) {
          return iterator.next();
        }
      }
    }
    return null;
  }

  private ServiceLoaderUtil() {}
}
