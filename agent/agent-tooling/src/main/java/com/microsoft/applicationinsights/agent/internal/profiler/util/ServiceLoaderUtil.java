package com.microsoft.applicationinsights.agent.internal.profiler.util;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ServiceLoaderUtil {

  public static <T> T findServiceLoader(Class<T> clazz) {
    ServiceLoader<T> factory = ServiceLoader.load(clazz);
    Iterator<T> iterator = factory.iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }
    return null;
  }
}
