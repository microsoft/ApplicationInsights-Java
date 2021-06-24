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

package com.microsoft.gcmonitor.collectors;

import com.microsoft.gcmonitor.GcEventConsumer;
import com.microsoft.gcmonitor.MemoryManagement;
import com.microsoft.gcmonitor.UnableToMonitorMemoryException;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollector;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollectorStats;
import com.microsoft.gcmonitor.garbagecollectors.GarbageCollectors;
import com.microsoft.gcmonitor.memorypools.MemoryPool;
import java.lang.management.GarbageCollectorMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

/** Implementation of a GarbageCollectorStats that provides access to the stats via a MxBean. */
public class JmxGarbageCollectorStats implements GarbageCollectorStats {

  private final List<MemoryPool> managedPools;
  private final IncrementalCounter countCounter;
  private final IncrementalCounter timeCounter;

  private final GarbageCollectorMXBean mxbean;
  private final ObjectName name;
  private final GcEventConsumer observer;
  private final MemoryManagement memoryManagement;
  private final GarbageCollector garbageCollector;

  public JmxGarbageCollectorStats(
      MemoryManagement memoryManagement,
      MBeanServerConnection connection,
      ObjectName name,
      GcEventConsumer observer)
      throws UnableToMonitorMemoryException {
    try {
      mxbean = JMX.newMXBeanProxy(connection, name, GarbageCollectorMXBean.class);
      this.name = name;
      managedPools = new ArrayList<>();
      countCounter = new IncrementalCounter();
      timeCounter = new IncrementalCounter();
      this.observer = observer;
      this.memoryManagement = memoryManagement;

      String gcName = (String) connection.getAttribute(name, "Name");
      this.garbageCollector = GarbageCollectors.create(gcName, this);
    } catch (Exception e) {
      throw new UnableToMonitorMemoryException("Failed to connect to MXBean", e);
    }
  }

  public void visitPools(Collection<MemoryPool> collection) {
    for (MemoryPool memoryPool : collection) {
      if (memoryPool.isManagedBy(garbageCollector)) {
        managedPools.add(memoryPool);
      }
    }
  }

  @Override
  public long getCollectionCount() {
    return Math.max(0, countCounter.getValue());
  }

  @Override
  public long getCollectionTime() {
    return Math.max(0, timeCounter.getValue());
  }

  public ObjectName getName() {
    return name;
  }

  public void update(Notification notification) {
    if (notification != null) {
      if (notification.getType().equals("com.sun.management.gc.notification")) {
        CompositeData data = (CompositeData) notification.getUserData();
        if (data.containsKey("gcInfo")) {

          countCounter.newValue(mxbean.getCollectionCount());
          timeCounter.newValue(mxbean.getCollectionTime());

          GcCollectionSample cs =
              new GcCollectionSample(
                  this.garbageCollector,
                  (CompositeData) data.get("gcInfo"),
                  (String) data.get("gcCause"),
                  (String) data.get("gcAction"),
                  memoryManagement);
          observer.accept(cs);
        }
      }
    }
  }

  public GarbageCollector getGarbageCollector() {
    return garbageCollector;
  }
}
