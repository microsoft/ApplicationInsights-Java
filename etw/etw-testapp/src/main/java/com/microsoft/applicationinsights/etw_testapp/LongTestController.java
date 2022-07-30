package com.microsoft.applicationinsights.etw_testapp;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.DiagnosticsLoggerProxy;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class LongTestController {
  private static final DiagnosticsLoggerProxy DIAGNOSTICS_LOGGER = new DiagnosticsLoggerProxy();
  private static final String DATE_FORMAT = "'on' yyyy-MM-dd 'at' HH:mm:ss z";

  private static final double NS_PER_MS = 1_000_000.0;

  @Autowired public TaskScheduler scheduler;

  private static class EventStats {
    private final int count;
    private final int exceptionCount;
    private final BigInteger totalNanos;
    private final BigInteger exceptionNanos;

    public EventStats() {
      this(0, 0, BigInteger.ZERO, BigInteger.ZERO);
    }

    public EventStats(
        int count, int exceptionCount, BigInteger totalNanos, BigInteger exceptionNanos) {
      this.count = count;
      this.exceptionCount = exceptionCount;
      this.totalNanos = totalNanos;
      this.exceptionNanos = exceptionNanos;
    }

    static EventStats incrementer(long nanos) {
      return new EventStats(1, 0, BigInteger.valueOf(nanos), BigInteger.ZERO);
    }

    static EventStats exceptionIncrementer(long nanos) {
      BigInteger value = BigInteger.valueOf(nanos);
      return new EventStats(1, 1, value, value);
    }

    @Override
    public String toString() {
      if (count == 0) {
        return "0 events";
      }
      double timeMs = totalNanos.doubleValue() / NS_PER_MS;
      double etimeMs = exceptionNanos.doubleValue() / NS_PER_MS;
      return String.format("%d events taking %.6f ms on average", count, timeMs / count)
          + (exceptionCount == 0
              ? ""
              : String.format(
                  ", %d events with exceptions taking %.6f ms on average",
                  exceptionCount, etimeMs / exceptionCount));
    }

    static BinaryOperator<EventStats> adder() {
      return new BinaryOperator<EventStats>() {
        @Override
        public EventStats apply(EventStats t, EventStats u) {
          return new EventStats(
              t.count + u.count,
              t.exceptionCount + u.exceptionCount,
              t.totalNanos.add(u.totalNanos),
              t.exceptionNanos.add(u.exceptionNanos));
        }
      };
    }
  }

  private static class TestStats {
    private final AtomicLong startTime = new AtomicLong();
    private final AtomicLong stopTime = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicReference<EventStats> infoStats = new AtomicReference<>(new EventStats());
    private final AtomicReference<EventStats> warnStats = new AtomicReference<>(new EventStats());
    private final AtomicReference<EventStats> errorStats = new AtomicReference<>(new EventStats());

    @Override
    public String toString() {
      SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
      return "<h1>Test "
          + (running.get() ? "Status" : "Results")
          + "</h1>\n"
          + "<p>Started: "
          + df.format(new Date(startTime.get()))
          + "</p>\n"
          + (running.get() ? "" : "<p>Stopped: " + df.format(new Date(stopTime.get())) + "</p>\n")
          + "<ul>\n"
          + "<li>Info Messages: "
          + infoStats.toString()
          + "</li>\n"
          + "<li>Warn Messages: "
          + warnStats.toString()
          + "</li>\n"
          + "<li>ErrorMessages: "
          + errorStats.toString()
          + "</li>\n"
          + "</ul>\n";
    }
  }

  private static class EventConfig {
    private final double chance;
    private final String message;

    public EventConfig(double chance, String message) {
      this.chance = chance;
      this.message = message;
    }

    @Override
    public String toString() {
      return String.format("%.2f%% chance, message='%s'", chance * 100, message);
    }
  }

  private static class TestConfig {
    private final EventConfig info = new EventConfig(1.0, "Info");
    private final EventConfig warn = new EventConfig(0.2, "Warning");
    private final EventConfig warnException = new EventConfig(0.5, "Warn Exception");
    private final EventConfig error = new EventConfig(1 / 15.0, "Error");
    private final EventConfig errorException = new EventConfig(0.5, "Error Exception");
    private Duration period;

    @Override
    public String toString() {
      return "<p>Period="
          + period
          + "</p>\n<ul>\n"
          + "<li>info events: "
          + info.toString()
          + "</li>\n"
          + "<li>warn events: "
          + warn.toString()
          + "<br/>\n"
          + "with exceptions: "
          + warnException.toString()
          + "</li>\n"
          + "<li>error events: "
          + error.toString()
          + "<br/>\n"
          + "with exceptions: "
          + errorException
          + "</li>\n"
          + "</ul>\n";
    }
  }

  private final AtomicReference<ScheduledFuture<?>> future = new AtomicReference<>();
  private TestStats currentStats = null;
  private TestStats previousStats = null;

  private final Object statsLock = new Object();

  private static class TestAction implements Runnable {
    private final TestConfig config;
    private final TestStats stats;

    public TestAction(TestConfig config, TestStats stats) {
      this.config = config;
      this.stats = stats;
    }

    @Override
    public void run() {
      if (rand() < config.info.chance) {
        long start = System.nanoTime();
        DIAGNOSTICS_LOGGER.info(config.info.message + (stats.infoStats.get().count + 1));
        stats.infoStats.accumulateAndGet(
            EventStats.incrementer(System.nanoTime() - start), EventStats.adder());
      }
      if (rand() < config.warn.chance) {
        long start = System.nanoTime();
        if (rand() < config.warnException.chance) {
          DIAGNOSTICS_LOGGER.warn(
              config.warn.message + (stats.warnStats.get().count + 1),
              new Exception(
                  config.warnException.message + (stats.warnStats.get().exceptionCount + 1)));
          stats.warnStats.accumulateAndGet(
              EventStats.exceptionIncrementer(System.nanoTime() - start), EventStats.adder());
        } else {
          DIAGNOSTICS_LOGGER.warn(config.warn.message + (stats.warnStats.get().count + 1));
          stats.warnStats.accumulateAndGet(
              EventStats.incrementer(System.nanoTime() - start), EventStats.adder());
        }
      }
      if (rand() < config.error.chance) {
        long start = System.nanoTime();
        if (rand() < config.errorException.chance) {
          DIAGNOSTICS_LOGGER.error(
              config.error.message + (stats.errorStats.get().count + 1),
              new Exception(
                  config.errorException.message + (stats.errorStats.get().exceptionCount + 1)));
          stats.errorStats.accumulateAndGet(
              EventStats.exceptionIncrementer(System.nanoTime() - start), EventStats.adder());
        } else {
          DIAGNOSTICS_LOGGER.error(config.error.message + (stats.errorStats.get().count + 1));
          stats.errorStats.accumulateAndGet(
              EventStats.incrementer(System.nanoTime() - start), EventStats.adder());
        }
      }
    }

    private double rand() {
      return RandomUtils.nextDouble(0, 1);
    }
  }

  @GetMapping("/start")
  public ResponseEntity<String> startTest(
      @RequestParam(name = "T", required = false, defaultValue = "1s") String periodStr) {
    if (future.get() != null) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Test already in progress.");
    }

    final Duration period;
    try {
      if (periodStr.toLowerCase().endsWith("ms")) {
        period = Duration.ofMillis(Long.parseLong(periodStr.substring(0, periodStr.length() - 2)));
      } else {
        period = Duration.parse("PT" + periodStr);
      }
    } catch (NumberFormatException | DateTimeParseException e) {
      return ResponseEntity.badRequest()
          .body(
              "<p>Period parameter 'T' could not parse \""
                  + periodStr
                  + "\"</p>"
                  + "<p><pre>"
                  + ExceptionUtils.getStackTrace(e)
                  + "</p>");
    }

    final long startTime;
    final ScheduledFuture<?> scheduledTask;
    final TestStats stats;
    final TestConfig config = new TestConfig();
    config.period = period;
    try {
      stats = new TestStats();
      PeriodicTrigger trigger = new PeriodicTrigger(period.toMillis());
      trigger.setInitialDelay(1000); // delay 1s
      scheduledTask = scheduler.schedule(new TestAction(config, stats), trigger);
      startTime = System.currentTimeMillis();
      if (!future.compareAndSet(null, scheduledTask)) {
        scheduledTask.cancel(true);
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Test already in progress");
      }
      stats.running.set(true);
      stats.startTime.set(startTime);
    } catch (RejectedExecutionException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT)
          .body("<p>Rejected.</p>\n<pre>" + ExceptionUtils.getStackTrace(e) + "</pre>");
    }
    synchronized (statsLock) {
      currentStats = stats;
    }
    return ResponseEntity.accepted()
        .body(
            "<p>Started test "
                + new SimpleDateFormat(DATE_FORMAT).format(new Date(startTime))
                + "</p>"
                + "<p>"
                + config.toString()
                + "</p>");
  }

  @GetMapping("/stop")
  public ResponseEntity<String> stopTest() {
    final TestStats cs;
    ScheduledFuture<?> f = future.get();
    if (f == null || !future.compareAndSet(f, null)) {
      return ResponseEntity.badRequest().body("Test is not running.");
    } else {
      if (!f.cancel(false)) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Test already stopped.");
      }
      synchronized (statsLock) {
        currentStats.stopTime.set(System.currentTimeMillis());
        currentStats.running.set(false);
        cs = currentStats;
        currentStats = null;
        previousStats = cs;
      }
    }
    return ResponseEntity.ok(cs.toString());
  }

  @GetMapping("/status")
  public ResponseEntity<String> testStatus() {
    final TestStats cs;
    synchronized (statsLock) {
      cs = currentStats;
    }
    if (cs == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No test running.");
    } else {
      return ResponseEntity.ok(cs.toString());
    }
  }

  @GetMapping("/results")
  public ResponseEntity<String> testResults() {
    final TestStats ps;
    synchronized (statsLock) {
      ps = previousStats;
    }
    if (ps == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No test results available.");
    } else {
      return ResponseEntity.ok(ps.toString());
    }
  }
}
