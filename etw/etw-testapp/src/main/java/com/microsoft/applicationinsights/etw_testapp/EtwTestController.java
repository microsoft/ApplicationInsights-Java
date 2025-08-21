package com.microsoft.applicationinsights.etw_testapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.applicationinsights.agent.internal.diagnostics.etw.DiagnosticsLoggerProxy;

@RestController
public class EtwTestController {
  private static final DiagnosticsLoggerProxy DIAGNOSTICS_LOGGER = new DiagnosticsLoggerProxy();

  private static final String LEVEL = "info";
  private static final boolean HAS_EXCEPTION = false;

  private final AtomicInteger errorCount = new AtomicInteger();
  private final AtomicInteger warnCount = new AtomicInteger();
  private final AtomicInteger infoCount = new AtomicInteger();

  @GetMapping("/log")
  public ResponseEntity<String> logPage() {
    String msg = "Hit /" + LEVEL + " ";
    int n;
    Throwable t = null;
    switch (LEVEL.toLowerCase()) {
      case "info":
        n = infoCount.incrementAndGet();
        DIAGNOSTICS_LOGGER.info(msg + n);
        break;
      case "error":
        n = errorCount.incrementAndGet();
        if (HAS_EXCEPTION) {
          t = new Exception("the error " + n);
          DIAGNOSTICS_LOGGER.error(msg + n, t);
        } else {
          DIAGNOSTICS_LOGGER.error(msg + n);
        }
        break;
      case "warn":
        n = warnCount.incrementAndGet();
        if (HAS_EXCEPTION) {
          t = new Exception("the warn " + n);
          DIAGNOSTICS_LOGGER.warn(msg + n, t);
        } else {
          DIAGNOSTICS_LOGGER.warn(msg + n);
        }
        break;
      default:
        return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(
        LEVEL.toUpperCase() + " " + n + (t == null ? "" : "<br/>\n" + t.toString()));
  }
}
