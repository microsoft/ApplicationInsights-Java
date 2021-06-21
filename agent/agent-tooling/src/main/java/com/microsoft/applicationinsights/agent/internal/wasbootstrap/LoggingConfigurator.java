package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.etw.EtwAppender;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsDiagnosticsLogFilter;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.ApplicationInsightsJsonLayout;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.log.MoshiJsonFormatter;
import com.microsoft.applicationinsights.agent.internal.wasbootstrap.configuration.Configuration;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Locale;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

class LoggingConfigurator {

    private final LoggerContext loggerContext;

    private final Level level;
    private final String destination;

    private final Path filePath;
    private final int fileMaxSizeMb;
    private final int fileMaxHistory;

    LoggingConfigurator(Configuration.SelfDiagnostics selfDiagnostics, Path agentPath) {
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        level = getLevel(selfDiagnostics.level);
        destination = selfDiagnostics.destination;
        filePath = agentPath.resolveSibling(selfDiagnostics.file.path);
        fileMaxSizeMb = selfDiagnostics.file.maxSizeMb;
        fileMaxHistory = selfDiagnostics.file.maxHistory;
    }

    void configure() {
        loggerContext.getLogger(ROOT_LOGGER_NAME).detachAndStopAllAppenders();

        if (DiagnosticsHelper.useAppSvcRpIntegrationLogging()) {
            configureAppSvcs();
        } else if (destination == null || destination.equalsIgnoreCase("file+console")) {
            configureFileAndConsole();
        } else if (destination.equalsIgnoreCase("file")) {
            configureFile();
        } else if (destination.equalsIgnoreCase("console")) {
            configureConsole();
        } else {
            throw new IllegalStateException("Unknown self-diagnostics destination: " + destination);
        }
    }

    private void configureAppSvcs() {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureFileAppender());
        rootLogger.addAppender(configureConsoleAppender());

        // Diagnostics IPA log file location. Disabled by default.
        String diagnosticsOutputDirectory = System.getenv(DiagnosticsHelper.APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY);
        if (diagnosticsOutputDirectory != null && !diagnosticsOutputDirectory.isEmpty()) {
            rootLogger.addAppender(configureDiagnosticAppender(diagnosticsOutputDirectory));
        }

        if (DiagnosticsHelper.isOsWindows()) {
            rootLogger.addAppender(configureEtwAppender());
        }
        // TODO what about linux?

        configureLoggingLevels();
    }

    private void configureFileAndConsole() {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureFileAppender());
        rootLogger.addAppender(configureConsoleAppender());

        configureLoggingLevels();
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private void configureFile() {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureFileAppender());

        configureLoggingLevels();
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private void configureConsole() {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureConsoleAppender());

        configureLoggingLevels();
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private Appender<ILoggingEvent> configureFileAppender() {
        Path logFileNamePath = filePath.getFileName();
        if (logFileNamePath == null) {
            throw new IllegalStateException("Unexpected empty self-diagnostics file path");
        }
        String logFileName = logFileNamePath.toString();
        String rollingFileName;
        int index = logFileName.lastIndexOf('.');
        if (index != -1) {
            rollingFileName = logFileName.substring(0, index) + ".%i" + logFileName.substring(index);
        } else {
            rollingFileName = logFileName + ".%i";
        }
        Path rollingFilePath = filePath.resolveSibling(rollingFileName);

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(loggerContext);
        appender.setName("FILE");
        appender.setFile(filePath.toString());

        FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setFileNamePattern(rollingFilePath.toString());
        rollingPolicy.setMinIndex(1);
        rollingPolicy.setMaxIndex(fileMaxHistory);
        rollingPolicy.setParent(appender);
        rollingPolicy.start();

        appender.setRollingPolicy(rollingPolicy);

        SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<>();
        triggeringPolicy.setContext(loggerContext);
        triggeringPolicy.setMaxFileSize(new FileSize(fileMaxSizeMb * 1024L * 1024L));
        triggeringPolicy.start();

        appender.setTriggeringPolicy(triggeringPolicy);

        appender.setEncoder(createEncoder());
        appender.start();

        return appender;
    }

    private Appender<ILoggingEvent> configureConsoleAppender() {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName("CONSOLE");

        appender.setEncoder(createEncoder());
        appender.start();

        return appender;
    }

    private Appender<ILoggingEvent> configureDiagnosticAppender(String diagnosticsOutputDirectory) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(loggerContext);
        appender.setName("DIAGNOSTICS_FILE");
        appender.setFile(diagnosticsOutputDirectory + "/applicationinsights-extension.log");

        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setContext(loggerContext);
        rollingPolicy.setFileNamePattern(diagnosticsOutputDirectory + "/applicationinsights-extension-%d{yyyy-MM-dd}.%i.log.old");
        rollingPolicy.setMaxHistory(1);
        rollingPolicy.setTotalSizeCap(new FileSize(10 * 1024 * 1024));
        rollingPolicy.setMaxFileSize(new FileSize(5 * 1024 * 1024));
        rollingPolicy.setParent(appender);
        rollingPolicy.start();

        appender.setRollingPolicy(rollingPolicy);

        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(loggerContext);

        ApplicationInsightsJsonLayout layout = new ApplicationInsightsJsonLayout();
        layout.setContext(loggerContext);
        layout.setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        layout.setTimestampFormatTimezoneId("Etc/UTC");
        layout.setAppendLineSeparator(true);

        MoshiJsonFormatter jsonFormatter = new MoshiJsonFormatter();
        jsonFormatter.setPrettyPrint(false);

        layout.setJsonFormatter(jsonFormatter);
        layout.start();

        encoder.setLayout(layout);
        encoder.start();

        appender.setEncoder(encoder);

        ApplicationInsightsDiagnosticsLogFilter filter = new ApplicationInsightsDiagnosticsLogFilter();
        filter.setContext(loggerContext);
        filter.start();

        appender.addFilter(filter);
        appender.start();

        return appender;
    }

    private Appender<ILoggingEvent> configureEtwAppender() {
        EtwAppender appender = new EtwAppender();
        appender.setContext(loggerContext);
        appender.setName("ETW_PROVIDER");

        ApplicationInsightsDiagnosticsLogFilter filter = new ApplicationInsightsDiagnosticsLogFilter();
        filter.setContext(loggerContext);
        filter.start();

        appender.addFilter(filter);
        appender.start();

        return appender;
    }

    private void configureLoggingLevels() {
        // never want to log apache http at trace or debug, it's just way to verbose
        Level atLeastInfoLevel = getMaxLevel(level, Level.INFO);

        Level otherLibsLevel = level == Level.INFO ? Level.WARN : level;

        // TODO need something more reliable, currently will log too much WARN if "muzzleMatcher" logger name changes
        // muzzleMatcher logs at WARN level in order to make them visible, but really should only be enabled when debugging
        Level muzzleMatcherLevel = level.toInt() <= Level.DEBUG.toInt() ? level : getMaxLevel(level, Level.ERROR);

        // never want to log apache http at trace or debug, it's just way to verbose
        loggerContext.getLogger("org.apache.http").setLevel(atLeastInfoLevel);
        // never want to log io.grpc.Context at trace or debug, as it logs confusing stack trace that looks like error but isn't
        loggerContext.getLogger("io.grpc.Context").setLevel(atLeastInfoLevel);
        // muzzleMatcher logs at WARN level, so by default this is OFF, but enabled when DEBUG logging is enabled
        loggerContext.getLogger("muzzleMatcher").setLevel(muzzleMatcherLevel);
        loggerContext.getLogger("com.microsoft.applicationinsights").setLevel(level);
        loggerContext.getLogger("com.azure.monitor.opentelemetry.exporter").setLevel(level);
        loggerContext.getLogger(ROOT_LOGGER_NAME).setLevel(otherLibsLevel);
    }

    private Encoder<ILoggingEvent> createEncoder() {
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSSX} %-5level %logger{36} - %msg%n");
        encoder.start();
        return encoder;
    }

    private static Level getMaxLevel(Level level1, Level level2) {
        return level1.toInt() >= level2.toInt() ? level1 : level2;
    }

    private static Level getLevel(String levelStr) {
        try {
            return Level.valueOf(levelStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unexpected self-diagnostic level: " + levelStr, e);
        }
    }
}
