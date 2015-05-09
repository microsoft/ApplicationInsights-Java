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

package com.microsoft.applicationinsights.core.volume;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Properties;

import com.microsoft.applicationinsights.channel.concrete.inprocess.InProcessTelemetryChannel;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;

/**
 * Created by gupele on 2/4/2015.
 */
public final class ThroughputTest {
    private final static int MIN_REQUESTS_TO_LOAD = 1000;
    private final static int MAX_REQUESTS_TO_LOAD = 1000000;
    private final static int MAX_TIME_TO_WAIT_IN_SECONDS = 100;
    private final static int DEFAULT_NUMBER_OF_TIMES = 3;
    private final static int DEFAULT_NUMBER_OF_PROPERTIES = 1;
    private final static String NUMBER_OF_TINES_TO_TEST_PROPERTY = "number.of.times.to.test";
    private final static String NUMBER_OF_CONTEXT_PROPERTIES = "number.of.context.properties";
    private final static String FIELD_TO_REPLACE = "s_transmitterFactory";

    private static Properties properties = new Properties();

    public static void main(String[] args) throws IOException {
        System.err.println("   ThroughputTest");
        if (!initialize()) {
            System.exit(1);
        }

        System.err.println("   Initialized successfully");
        ArrayList<ArrayList<TestStats>> allStats = new ArrayList<ArrayList<TestStats>>();

        test(allStats);

        printStats(allStats);
    }

    private static void test(ArrayList<ArrayList<TestStats>> allStats) {
        int numberOfTimesToTest = DEFAULT_NUMBER_OF_TIMES;
        try {
            numberOfTimesToTest = Integer.valueOf(properties.getProperty(NUMBER_OF_TINES_TO_TEST_PROPERTY));
        } catch (NumberFormatException e) {
        }
        int numberOfContextProperties = DEFAULT_NUMBER_OF_PROPERTIES;
        try {
            numberOfContextProperties = Integer.valueOf(properties.getProperty(NUMBER_OF_CONTEXT_PROPERTIES));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        Tester tester = new Tester(numberOfContextProperties);
        for (int i = 0; i < numberOfTimesToTest; ++i) {
            System.err.println("   Running iteration " + (i + 1));
            ArrayList<TestStats> stats = new ArrayList<TestStats>();
            allStats.add(stats);
            for (int numberOfEvents = MIN_REQUESTS_TO_LOAD; numberOfEvents <= MAX_REQUESTS_TO_LOAD; numberOfEvents *= 10) {
                tester.reset(numberOfEvents, MAX_TIME_TO_WAIT_IN_SECONDS);
                tester.sendTelemetries();
                stats.add(tester.getResults());
            }
        }
    }

    private static void printStats(ArrayList<ArrayList<TestStats>> allStats) {
        int counter = 0;
        System.err.println("--------------------------------------------");
        System.err.println("---------------     Results      -----------");
        System.err.println("--------------------------------------------");
        for (ArrayList<TestStats> oneIteration : allStats) {
            ++counter;

            System.err.println();
            System.err.println("      Iteration: " + counter);
            System.err.println("===========================================");
            for (TestStats stat : oneIteration) {
                System.err.println("Result                         : " + stat.getStatus());
                System.err.println("Sent                           : " + stat.getNumberOfSentEvents());
                System.err.println("Accepted                       : " + stat.getNumberOfAcceptedEvents());
                System.err.println("Send time(Seconds)             : " + stat.getSendTimeInSeconds());
                System.err.println("Accepted until end of sending  : " + stat.getAcceptedUntilEndOfSending());
                System.err.println("Time (Seconds)                 : " + stat.getTimeToFinishInSeconds());
                System.err.println("Average events/second          : " + stat.getEventsPerSecond());
                System.err.println();
                System.err.println("--------------------------------------------");
            }
        }
    }

    /**
     * Initialize the test
     * Change the channel by reflection - a must.
     * Read the configuration properties file - if we fail, we will use default values.
     * @return False if we can't continue with the testing.
     */
    private static boolean initialize() {
        if (!setTestHookInChannel()) {
            System.err.println("Failed to initialize test, exiting");
            return false;
        }

        if (!parseConfiguration()) {
            System.err.println("Failed to read properties, will use default values");
        }

        return true;
    }

    private static boolean parseConfiguration() {
        String fileName = "Volume.properties";

        // Trying to load configuration as a resource.
        ClassLoader classLoader = ThroughputTest.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        // If not found as a resource, trying to load from the executing jar directory
        if (inputStream == null) {
            try {
                String jarFullPath = ThroughputTest.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
                File jarFile = new File(jarFullPath);

                if (jarFile.exists()) {
                    String jarDirectory = jarFile.getParent();
                    String configurationPath = jarDirectory + "/" + fileName;

                    inputStream = new FileInputStream(configurationPath);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
                return false;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }

        if (inputStream != null) {
            try {
                properties = new Properties();
                properties.load(inputStream);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    };

    private static boolean setTestHookInChannel() {
        for (Field f : InProcessTelemetryChannel.class.getDeclaredFields()) {
            if (FIELD_TO_REPLACE.equals(f.getName())) {
                try {
                    f.setAccessible(true);
                    TransmitterFactory tf = new ThroughputTestTransmitterFactory();
                    f.set(f, tf);
                    return true;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } finally {
                    f.setAccessible(false);
                }
            }
        }

        return false;
    }
}
