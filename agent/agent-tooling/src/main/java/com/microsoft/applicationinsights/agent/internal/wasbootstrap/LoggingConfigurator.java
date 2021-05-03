package com.microsoft.applicationinsights.agent.internal.wasbootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.FileSize;
import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.DiagnosticsHelper;

import java.nio.file.Path;
import java.util.Locale;

import static org.slf4j.Logger.ROOT_LOGGER_NAME;

public class LoggingConfigurator {

    public static String level;
    public static String destination;

    public static Path filePath;
    public static int fileMaxSizeMb;
    public static int fileMaxHistory;

    public void configure(LoggerContext loggerContext) {

        loggerContext.getLogger(ROOT_LOGGER_NAME).detachAndStopAllAppenders();

        if (DiagnosticsHelper.useAppSvcRpIntegrationLogging()) {
            configureAppSvcs(loggerContext);
        } else if (destination == null || destination.equalsIgnoreCase("file+console")) {
            configureFileAndConsole(loggerContext);
        } else if (destination.equalsIgnoreCase("file")) {
            configureFile(loggerContext);
        } else if (destination.equalsIgnoreCase("console")) {
            configureConsole(loggerContext);
        } else {
            throw new IllegalStateException("Unknown self-diagnostics destination: " + destination);
        }
    }

    private void configureAppSvcs(LoggerContext loggerContext) {
        // Diagnostics IPA log file location. Disabled by default.
        final String internalLogOutputLocation = System.getenv(DiagnosticsHelper.APPLICATIONINSIGHTS_DIAGNOSTICS_OUTPUT_DIRECTORY);
        if (internalLogOutputLocation == null || internalLogOutputLocation.isEmpty()) {
            System.setProperty("ai.config.appender.diagnostics.location", "");
        }

        // Diagnostics IPA ETW provider. Windows-only.
        if (!DiagnosticsHelper.isOsWindows()) {
            System.setProperty("ai.config.appender.etw.location", "");
        }

        // FIXME (trask)



        configureLoggingLevels(loggerContext);
    }

    private void configureFileAndConsole(LoggerContext loggerContext) {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureFileAppender(loggerContext));
        rootLogger.addAppender(configureConsoleAppender(loggerContext));

        configureLoggingLevels(loggerContext);
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private void configureFile(LoggerContext loggerContext) {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureFileAppender(loggerContext));

        configureLoggingLevels(loggerContext);
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private void configureConsole(LoggerContext loggerContext) {
        Logger rootLogger = loggerContext.getLogger(ROOT_LOGGER_NAME);
        rootLogger.addAppender(configureConsoleAppender(loggerContext));

        configureLoggingLevels(loggerContext);
        // these messages are specifically designed for attach
        loggerContext.getLogger("applicationinsights.extension.diagnostics").setLevel(Level.OFF);
    }

    private Appender<ILoggingEvent> configureFileAppender(LoggerContext loggerContext) {
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

        appender.setEncoder(createEncoder(loggerContext));
        appender.start();

        return appender;
    }

    private Appender<ILoggingEvent> configureConsoleAppender(LoggerContext loggerContext) {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        appender.setContext(loggerContext);
        appender.setName("CONSOLE");

        appender.setEncoder(createEncoder(loggerContext));
        appender.start();

        return appender;
    }

    private static void configureLoggingLevels(LoggerContext loggerContext) {

        Level level = getLevel(LoggingConfigurator.level);

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
        loggerContext.getLogger(ROOT_LOGGER_NAME).setLevel(otherLibsLevel);
    }

    private static Encoder<ILoggingEvent> createEncoder(LoggerContext loggerContext) {
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
            throw new IllegalStateException("Unexpected self-diagnostic level: " + levelStr);
        }
    }
}
