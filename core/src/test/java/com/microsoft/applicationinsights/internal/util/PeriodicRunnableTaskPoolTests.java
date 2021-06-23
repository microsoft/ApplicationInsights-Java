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

package com.microsoft.applicationinsights.internal.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;

class PeriodicRunnableTaskPoolTests {

  private PeriodicTaskPool taskPool = null;

  @BeforeEach
  void initialize() {
    taskPool = new PeriodicTaskPool(1, "test-pool");
  }

  @AfterEach
  void cleanUp() {
    taskPool.stop(5, TimeUnit.SECONDS);
    taskPool = null;
  }

  @Test
  void testNewTaskCanBeSubmittedAndIsRunning() {
    SignalListener signal = new SignalListener();
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new TestRunnable(signal), 0, 1, TimeUnit.SECONDS, "Test");
    ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    assertThat(future.isCancelled()).isFalse();
    assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();
    sleep(1, TimeUnit.SECONDS);
    assertThat(signal.isDone()).isTrue();
  }

  @Test
  void testMultipleTaskCanBeSubmittedAndAreRunning() {
    SignalListener sig1 = new SignalListener();
    SignalListener sig2 = new SignalListener();
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new TestRunnable(sig1), 0, 1, TimeUnit.SECONDS, "Test");
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask2 =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new TestRunnable(sig2), 0, 1, TimeUnit.SECONDS, "Test1");
    ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    ScheduledFuture<?> future1 = taskPool.executePeriodicRunnableTask(periodicRunnableTask2);
    assertThat(future.isCancelled()).isFalse();
    assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();
    assertThat(future1.isCancelled()).isFalse();
    assertThat((Future<?>) taskPool.getTask(periodicRunnableTask2)).isNotNull();

    sleep(1, TimeUnit.SECONDS);
    assertThat(sig1.isDone()).isTrue();
    assertThat(sig2.isDone()).isTrue();
  }

  @Test
  void multipleTasksWithSameIdCannotBeSubmitted() {
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new DoNothingRunnable(), 0, 1, TimeUnit.SECONDS, "Test");
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask2 =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new DoNothingRunnable(), 0, 1, TimeUnit.SECONDS, "Test");
    taskPool.executePeriodicRunnableTask(periodicRunnableTask);

    assertThatThrownBy(() -> taskPool.executePeriodicRunnableTask(periodicRunnableTask2))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void canCancelSubmittedTask() {
    SignalListener signal = new SignalListener();
    PeriodicTaskPool.PeriodicRunnableTask periodicRunnableTask =
        PeriodicTaskPool.PeriodicRunnableTask.createTask(
            new TestRunnable(signal), 0, 10, TimeUnit.SECONDS, "Test");
    ScheduledFuture<?> future = taskPool.executePeriodicRunnableTask(periodicRunnableTask);
    assertThat(future.isCancelled()).isFalse();
    assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNotNull();

    taskPool.cancelPeriodicTask(periodicRunnableTask);
    assertThat(future.isCancelled()).isTrue();
    assertThat((Future<?>) taskPool.getTask(periodicRunnableTask)).isNull();

    sleep(10, TimeUnit.SECONDS);
    assertThat(signal.isDone()).isFalse();
  }

  @Test
  void cannotScheduleNullRunnable() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    null, 0, 1, TimeUnit.SECONDS, "Test"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotHaveNegativeInitialDelay() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    new DoNothingRunnable(), -1, 1, TimeUnit.SECONDS, "Test"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotHaveNegativeRepeatDuration() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    new DoNothingRunnable(), 0, -1, TimeUnit.SECONDS, "Test"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotHaveNullTaskId() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    new DoNothingRunnable(), 0, 1, TimeUnit.SECONDS, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotHaveEmptyTaskId() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    new DoNothingRunnable(), 0, 1, TimeUnit.SECONDS, ""))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void cannotHaveNullTimeUnit() {
    assertThatThrownBy(
            () ->
                PeriodicTaskPool.PeriodicRunnableTask.createTask(
                    new DoNothingRunnable(), 0, 1, null, "Test"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static void sleep(int delay, TimeUnit unit) {
    try {
      unit.sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private interface CompletionListener {
    void complete();
  }

  private static class SignalListener implements CompletionListener {
    private volatile boolean done = false;

    @Override
    public void complete() {
      synchronized (this) {
        done = true;
      }
    }

    synchronized boolean isDone() {
      return done;
    }
  }

  private static class DoNothingRunnable implements Runnable {
    @Override
    public void run() {}
  }

  private static class TestRunnable implements Runnable {
    private final CompletionListener listener;

    TestRunnable(CompletionListener listener) {
      this.listener = listener;
    }

    @Override
    public void run() {
      listener.complete();
    }
  }
}
