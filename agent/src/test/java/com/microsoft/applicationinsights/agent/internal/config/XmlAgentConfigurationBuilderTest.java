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

package com.microsoft.applicationinsights.agent.internal.config;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;

import static org.junit.Assert.*;

public final class XmlAgentConfigurationBuilderTest {
    private final static String TEMP_TEST_FOLDER = "AgentTests";
    private final static String TEMP_CONF_FILE = "AI-Agent.xml";

    private interface Printer {
        void print(PrintWriter writer);
    }

    private static class MalformedXmlTestPrinter implements Printer {
        @Override
        public void print(PrintWriter writer) {
            writer.println("<BuiltIn>");
        }
    }

    private static class BuiltInTestPrinter implements Printer {
        @Override
        public void print(PrintWriter writer) {
            writer.println("<BuiltIn>");

            writer.println("<HIBERNATE enabled=\"false\"/>");
            writer.println("<HTTP enabled=\"true\"/>");

            writer.println("</BuiltIn>");
        }
    }

    private static class ConfigurationTestPrinter implements Printer {
        @Override
        public void print(PrintWriter writer) {
            writer.println("<Class name=\"a.AClass1\">");

            writer.println("<Method name=\"method1\" reportExecutionTime=\"true\">");
            writer.println("</Method>");
            writer.println("<Method name=\"method2\" enabled=\"false\" reportExecutionTime=\"true\">");
            writer.println("</Method>");
            writer.println("<Method name=\"method3\" reportCaughtExceptions=\"true\">");
            writer.println("</Method>");

            writer.println("</Class>");

            writer.println("<Class name=\"a.AClass2\">");
            writer.println("</Class>");

            writer.println("<Class name=\"a.AClass3\" enabled=\"false\">");
            writer.println("<Method name=\"method1\" reportExecutionTime=\"true\">");
            writer.println("</Method>");
            writer.println("<Method name=\"method2\" enabled=\"true\" reportExecutionTime=\"true\">");
            writer.println("</Method>");
            writer.println("<Method name=\"method3\" reportCaughtExceptions=\"true\">");
            writer.println("</Method>");
            writer.println("</Class>");
        }
    }

    @Test
    public void testMalformedConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration(new MalformedXmlTestPrinter());
        assertNull(configuration);
    }

    @Test
    public void testClassesConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration(new ConfigurationTestPrinter());
        HashMap<String, ClassInstrumentationData> classes = configuration.getRequestedClassesToInstrument();
        assertNotNull(classes);
        assertEquals(classes.size(), 2);
    }

    @Test
    public void testBuiltInConfiguration() throws IOException {
        AgentConfiguration configuration = testConfiguration(new BuiltInTestPrinter());
        AgentBuiltInConfiguration builtInConfiguration = configuration.getBuiltInSwitches();
        assertEquals(builtInConfiguration.isEnabled(), true);
        assertEquals(builtInConfiguration.isHttpEnabled(), true);
        assertEquals(builtInConfiguration.isJdbcEnabled(), true);
        assertEquals(builtInConfiguration.isJdbcEnabled(), true);
        assertEquals(builtInConfiguration.isHibernateEnabled(), false);
    }

    private AgentConfiguration testConfiguration(Printer printer) throws IOException {
        File folder = null;
        try {
            folder = createFolder();
            createConfigurationFileTest(folder, printer);
            return new XmlAgentConfigurationBuilder().parseConfigurationFile(folder.toString());
        } finally {
            cleanFolder(folder);
        }
    }

    private void createConfigurationFileTest(File folder, Printer printer) throws IOException {
        PrintWriter writer = null;
        try {
            File file = new File(folder, TEMP_CONF_FILE);
            if (file.exists()) {
                file.delete();
            }

            file.createNewFile();
            writer = createWriter(file);
            printStart(writer);
            printer.print(writer);
            printEnd(writer);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void printEnd(PrintWriter writer) {
        writer.println("</Instrumentation>");
        writer.println("</ApplicationInsightsAgent>");
    }

    private void printStart(PrintWriter writer) {
        writer.println("<ApplicationInsightsAgent>");
        writer.println("<Instrumentation>");
    }

    private PrintWriter createWriter(File file) throws FileNotFoundException, UnsupportedEncodingException {
        return new PrintWriter(file, "UTF-8");
    }

    private File createFolder() throws IOException {
        File folder;
        String filesPath = System.getProperty("java.io.tmpdir") + File.separator + TEMP_TEST_FOLDER;
        folder = new File(filesPath);
        if (folder.exists()) {
            try {
                FileUtils.deleteDirectory(folder);
            } catch (Throwable t) {
            }
        }
        if (!folder.exists()) {
            folder.mkdir();
        }

        return folder;
    }

    private void cleanFolder(File folder) {
        if (folder != null && folder.exists()) {
            try {
                File file = new File(folder, TEMP_CONF_FILE);
                if (file.exists()) {
                    file.delete();
                }
                FileUtils.deleteDirectory(folder);
            } catch (IOException e) {
            }
        }
    }
}