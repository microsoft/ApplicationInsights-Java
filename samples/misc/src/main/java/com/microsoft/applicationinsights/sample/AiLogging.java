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

package com.microsoft.applicationinsights.sample;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;

import java.io.IOException;

@SuppressWarnings("ALL")
public class AiLogging {
    public static void main(String[] args) throws IOException {

        System.out.println("This program sends 2 events for each of the 3 supported logging technology.");

        TelemetryClient appInsights = new TelemetryClient();
        if (args.length > 0) {
            appInsights.getContext().setInstrumentationKey(args[0]);
        }
        String iKey = appInsights.getContext().getInstrumentationKey();
        if (iKey == null)
        {
            System.out.println("Error: no iKey set in ApplicationInsights.xml or as a parameter for this program.");
            return;
        }
        System.out.println("Application iKey set to " + appInsights.getContext().getInstrumentationKey());
        TelemetryConfiguration.getActive().getChannel().setDeveloperMode(true);

        // Log4j 1.2
        System.out.println("\n--- Logging using Log4j v1.2 ---");
        org.apache.log4j.Logger log4j12Logger = org.apache.log4j.LogManager.getRootLogger();
        log4j12Logger.trace("New Log4j 1.2 event!");
        System.out.println("    Trace level log sent via log4j v1.2 logger.");
        log4j12Logger.info("info: New Log4j 1.2 event!");
        System.out.println("    Info level log sent via log4j v1.2 logger.");

        // Log4j 2
        System.out.println("\n--- Logging using Log4j v2 ---");
        org.apache.logging.log4j.Logger log4j2Logger = org.apache.logging.log4j.LogManager.getRootLogger();
        log4j2Logger.trace("New Log4j 2 event!");
        System.out.println("    Trace level log sent via log4j v2 logger.");
        log4j2Logger.info("info: New Log4j 2 event!");
        System.out.println("    Info level log sent via log4j v2 logger.");

        // Logback
        System.out.println("\n--- Logging using Logback ---");
        ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("root");
        logbackLogger.trace("New Logback event!");
        System.out.println("    Trace level log sent via logback logger.");
        logbackLogger.info("info: New Logback event!");
        System.out.println("    Trace level log sent via logback logger.");
    }
}
