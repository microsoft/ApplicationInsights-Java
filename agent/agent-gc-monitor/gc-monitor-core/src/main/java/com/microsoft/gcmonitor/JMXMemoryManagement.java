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

package com.microsoft.gcmonitor;

import static java.lang.management.ManagementFactory.RUNTIME_MXBEAN_NAME;
import static java.util.Collections.unmodifiableCollection;

import com.microsoft.gcmonitor.collectors.JmxGarbageCollectorStats;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import com.microsoft.gcmonitor.memorypools.MemoryPools;
import com.microsoft.gcmonitor.notifications.NotificationObserver;
import java.io.IOException;
import java.lang.management.RuntimeMXBean;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/** Implementation of MemoryManagement with MxBean as the source of data */
public class JMXMemoryManagement implements MemoryManagement {

  private static final String POOL_MXBEANS = "java.lang:type=MemoryPool,name=*";
  private static final String COLLECTOR_MXBEANS = "java.lang:type=GarbageCollector,name=*";

  private Set<MemoryPool> pools;
  private Set<JmxGarbageCollectorStats> collectors;
  private RuntimeMXBean runtimeBean;
  private MemoryManagers collectorGroup;

  public static JMXMemoryManagement create(
      MBeanServerConnection connection, ExecutorService executorService, GCEventConsumer consumer)
      throws UnableToMonitorMemoryException {
    return new JMXMemoryManagement()
        .init(connection, consumer)
        .monitorMxBeans(connection, executorService);
  }

  protected JMXMemoryManagement init(MBeanServerConnection connection, GCEventConsumer consumer)
      throws UnableToMonitorMemoryException {
    runtimeBean = initRuntime(connection);
    collectors = initCollectors(connection, consumer);

    Set<GarbageCollector> col =
        collectors.stream()
            .map(JmxGarbageCollectorStats::getGarbageCollector)
            .collect(Collectors.toSet());

    pools = initPools(connection, col);

    for (JmxGarbageCollectorStats collector : collectors) {
      collector.visitPools(pools);
    }
    this.collectorGroup = MemoryManagers.of(this);

    return this;
  }

  private static RuntimeMXBean initRuntime(MBeanServerConnection connection)
      throws UnableToMonitorMemoryException {
    try {
      return JMX.newMXBeanProxy(
          connection, new ObjectName(RUNTIME_MXBEAN_NAME), RuntimeMXBean.class);
    } catch (MalformedObjectNameException | NullPointerException e) {
      throw new UnableToMonitorMemoryException(e);
    }
  }

  protected JMXMemoryManagement monitorMxBeans(
      MBeanServerConnection connection, ExecutorService executorService)
      throws UnableToMonitorMemoryException {
    NotificationObserver observer = new NotificationObserver(executorService);
    observer.watchGcNotificationEvents();
    try {
      collectors.forEach(
          collector -> {
            try {
              connection.addNotificationListener(collector.getName(), observer, null, collector);
            } catch (Exception e) {
              throw new IllegalStateException(e);
            }
          });
    } catch (RuntimeException e) {
      throw new UnableToMonitorMemoryException(e);
    }

    return this;
  }

  private Set<JmxGarbageCollectorStats> initCollectors(
      MBeanServerConnection connection, GCEventConsumer consumer)
      throws UnableToMonitorMemoryException {
    return getEntityFromMbeanServer(
        COLLECTOR_MXBEANS, connection, name -> getJmxGarbageCollector(connection, consumer, name));
  }

  public JmxGarbageCollectorStats getJmxGarbageCollector(
      MBeanServerConnection connection, GCEventConsumer consumer, ObjectName name)
      throws UnableToMonitorMemoryException {
    return new JmxGarbageCollectorStats(this, connection, name, consumer);
  }

  private static Set<MemoryPool> initPools(
      MBeanServerConnection connection, Set<GarbageCollector> garbageCollectors)
      throws UnableToMonitorMemoryException {
    return getEntityFromMbeanServer(
        POOL_MXBEANS,
        connection,
        name -> MemoryPools.getMemoryPool(connection, name, garbageCollectors));
  }

  interface CollectorFactory<T, V> {
    V apply(T t)
        throws UnableToMonitorMemoryException, AttributeNotFoundException, MBeanException,
            ReflectionException, InstanceNotFoundException, IOException;
  }

  private static <V> Set<V> getEntityFromMbeanServer(
      String beanName, MBeanServerConnection connection, CollectorFactory<ObjectName, V> factory)
      throws UnableToMonitorMemoryException {
    try {
      ObjectName pattern = new ObjectName(beanName);
      Set<V> domainObjects = new HashSet<>();
      for (ObjectName name : connection.queryNames(pattern, null)) {
        V domainObject = factory.apply(name);
        domainObjects.add(domainObject);
      }
      return domainObjects;
    } catch (Exception e) {
      throw new UnableToMonitorMemoryException("Unable to initialise memory", e);
    }
  }

  @Override
  public Collection<MemoryPool> getPools() {
    return unmodifiableCollection(pools);
  }

  @Override
  public Optional<MemoryPool> getPool(String name) {
    return pools.stream().filter(pool -> pool.getName().equals(name)).findFirst();
  }

  @Override
  public Set<GarbageCollector> getCollectors() {
    return collectors.stream()
        .map(JmxGarbageCollectorStats::getGarbageCollector)
        .collect(Collectors.toSet());
  }

  @Override
  public long getUptime() {
    return runtimeBean.getUptime();
  }

  @Override
  public MemoryManagers getCollectorGroup() {
    return collectorGroup;
  }
}
