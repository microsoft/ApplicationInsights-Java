package com.microsoft.applicationinsights.etw_testapp;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.DiagnosticsLoggerProxy;

@RestController
public class EtwTestController {
  private static final DiagnosticsLoggerProxy DIAGNOSTICS_LOGGER = new DiagnosticsLoggerProxy();

  private final AtomicInteger errorCount = new AtomicInteger();
  private final AtomicInteger warnCount = new AtomicInteger();
  private final AtomicInteger infoCount = new AtomicInteger();

  @GetMapping("/{level}")
  public ResponseEntity<String> logPage(
      @PathVariable String level,
      @RequestParam(name = "e", required = false, defaultValue = "false") boolean hasException) {
    String msg = "Hit /" + level + " ";
    int n;
    Throwable t = null;
    switch (level.toLowerCase()) {
      case "info":
        n = infoCount.incrementAndGet();
        DIAGNOSTICS_LOGGER.info(msg + n);
        break;
      case "error":
        n = errorCount.incrementAndGet();
        if (hasException) {
          t = new Exception("the error " + n);
          DIAGNOSTICS_LOGGER.error(msg + n, t);
        } else {
          DIAGNOSTICS_LOGGER.error(msg + n);
        }
        break;
      case "warn":
        n = warnCount.incrementAndGet();
        if (hasException) {
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
        level.toUpperCase() + " " + n + (t == null ? "" : "<br/>\n" + t.toString()));
  }
}
