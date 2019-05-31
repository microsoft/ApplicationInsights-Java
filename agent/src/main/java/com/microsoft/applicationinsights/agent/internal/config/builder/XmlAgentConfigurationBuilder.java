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
package com.microsoft.applicationinsights.agent.internal.config.builder;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.ClassInstrumentationData;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XmlAgentConfigurationBuilder {

    private final static String AGENT_XML_CONFIGURATION_NAME = "AI-Agent.xml";

    private final static String MAIN_TAG = "ApplicationInsightsAgent";
    private final static String INSTRUMENTATION_TAG = "Instrumentation";
    private final static String CLASS_TAG = "Class";
    private final static String METHOD_TAG = "Method";

    private final static String BUILT_IN_TAG = "BuiltIn";
    private final static String JEDIS_TAG = "Jedis";
    private final static String HTTP_TAG = "HTTP";
    private final static String JDBC_TAG = "JDBC";
    private final static String JMX_TAG = "AgentJmx";
    private final static String MAX_STATEMENT_QUERY_LIMIT_TAG = "MaxStatementQueryLimitInMS";

    // visible for testing
    private final static String AGENT_LOGGER_TAG = "AgentLogger";
    private final static String SDK_LOGGER_TYPE_TAG = "type";
    private final static String SDK_LOG_LEVEL_TAG = "Level";
    private final static String SDK_LOGGER_UNIQUE_PREFIX_TAG = "UniquePrefix";
    private final static String SDK_LOGGER_BASE_FOLDER_PATH_TAG = "BaseFolderPath";
    private final static String SDK_LOGGER_MAX_NUMBER_OF_LOG_FILES = "NumberOfFiles";
    private final static String SDK_LOGGER_NUMBER_OF_TOTAL_SIZE_IN_MB = "NumberOfTotalSizeInMB";

    private final static String W3C_ENABLED = "W3C";
    private final static String W3C_BACKCOMPAT_PARAMETER = "enableW3CBackCompat";

    private final static String EXCLUDED_PREFIXES_TAG = "ExcludedPrefixes";

    private final static String RUNTIME_EXCEPTION_TAG = "RuntimeException";

    private final static String THRESHOLD_ATTRIBUTE = "thresholdInMS";
    private final static String ENABLED_ATTRIBUTE = "enabled";
    private final static String NAME_ATTRIBUTE = "name";
    private final static String REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE = "reportCaughtExceptions";
    private final static String REPORT_EXECUTION_TIME_ATTRIBUTE = "reportExecutionTime";
    private final static String SIGNATURE_ATTRIBUTE = "signature";

    public AgentConfiguration parseConfigurationFile(String baseFolder) {
        AgentConfiguration agentConfiguration = new AgentConfiguration();

        String configurationFileName = baseFolder;
        if (!baseFolder.endsWith(File.separator)) {
            configurationFileName += File.separator;
        }
        configurationFileName += AGENT_XML_CONFIGURATION_NAME;

        File configurationFile = new File(configurationFileName);
        if (!configurationFile.exists()) {
            InternalLogger.INSTANCE.trace("Did not find Agent configuration file in '%s'", configurationFileName);
            return agentConfiguration;
        }

        InternalLogger.INSTANCE.trace("Found Agent configuration file in '%s'", configurationFileName);
        try {
            Element topElementTag = getTopTag(configurationFile);
            if (topElementTag == null) {
                return agentConfiguration;
            }

            initializeAgentLogger(topElementTag);

            getForbiddenPaths(topElementTag);

            Element instrumentationTag = getInstrumentationTag(topElementTag);
            if (instrumentationTag == null) {
                return agentConfiguration;
            }

            setBuiltInInstrumentation(agentConfiguration, instrumentationTag);

            NodeList addClasses = getAllClassesToInstrument(instrumentationTag);
            if (addClasses == null) {
                return agentConfiguration;
            }

            HashMap<String, ClassInstrumentationData> classesToInstrument = new HashMap<>();
            for (int index = 0; index < addClasses.getLength(); ++index) {
                Element classElement = getClassDataElement(addClasses.item(index));
                if (classElement == null) {
                    continue;
                }

                ClassInstrumentationData data = getClassInstrumentationData(classElement, classesToInstrument);
                if (data == null) {
                    continue;
                }

                addMethods(data, classElement);
                if (data.isEmpty() && !data.isReportCaughtExceptions() && !data.isReportExecutionTime()) {
                    classesToInstrument.remove(data.getClassName());
                }
            }

            agentConfiguration.setClassesToInstrument(classesToInstrument);
            return agentConfiguration;
        } catch (ThreadDeath td) {
            throw td;
        } catch (Throwable e) {
            try {
                InternalLogger.INSTANCE.error("Exception while parsing Agent configuration file: '%s'", e.toString());
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
            return null;
        }
    }

    private void getForbiddenPaths(Element parent) {
        NodeList nodes = parent.getElementsByTagName(EXCLUDED_PREFIXES_TAG);
        Element forbiddenElement = XmlParserUtils.getFirst(nodes);
        if (forbiddenElement != null) {
            InternalLogger.INSTANCE.warn(EXCLUDED_PREFIXES_TAG + " tag in AI-Agent.xml is no longer used");
        }
    }

    private void setBuiltInInstrumentation(AgentConfiguration agentConfiguration,
                                           Element instrumentationTags) {
        AgentBuiltInConfigurationBuilder builtInConfigurationBuilder = new AgentBuiltInConfigurationBuilder();

        NodeList nodes = instrumentationTags.getElementsByTagName(BUILT_IN_TAG);
        Element builtInElement = XmlParserUtils.getFirst(nodes);
        if (builtInElement == null) {
            builtInConfigurationBuilder.setEnabled(false);
            agentConfiguration.setBuiltInData(builtInConfigurationBuilder.create());
            return;
        }

        boolean builtInIsEnabled = XmlParserUtils.getEnabled(builtInElement, BUILT_IN_TAG);
        builtInConfigurationBuilder.setEnabled(builtInIsEnabled);
        if (!builtInIsEnabled) {
            builtInConfigurationBuilder.setEnabled(false);
            agentConfiguration.setBuiltInData(builtInConfigurationBuilder.create());
            return;
        }

        nodes = builtInElement.getElementsByTagName(JEDIS_TAG);
        Element element = XmlParserUtils.getFirst(nodes);
        builtInConfigurationBuilder.setJedisEnabled(XmlParserUtils.getEnabled(element, JEDIS_TAG));

        nodes = builtInElement.getElementsByTagName(RUNTIME_EXCEPTION_TAG);
        Element rtExceptionElement = XmlParserUtils.getFirst(nodes);
        if (rtExceptionElement != null) {
            InternalLogger.INSTANCE.warn(RUNTIME_EXCEPTION_TAG + " tag in AI-Agent.xml is no longer used");
        }

        nodes = builtInElement.getElementsByTagName(HTTP_TAG);
        Element httpElement = XmlParserUtils.getFirst(nodes);
        boolean isW3CEnabled = XmlParserUtils.w3cEnabled(httpElement, W3C_ENABLED, false);
        boolean isW3CBackportEnabled = XmlParserUtils.w3cEnabled(httpElement, W3C_BACKCOMPAT_PARAMETER, true);
        builtInConfigurationBuilder.setHttpEnabled(XmlParserUtils.getEnabled(element, HTTP_TAG), isW3CEnabled,
                isW3CBackportEnabled);

        nodes = builtInElement.getElementsByTagName(JDBC_TAG);
        builtInConfigurationBuilder.setJdbcEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), JDBC_TAG));

        nodes = builtInElement.getElementsByTagName(JMX_TAG);
        Element jmxElement = XmlParserUtils.getFirst(nodes);
        if (jmxElement != null) {
            InternalLogger.INSTANCE.warn(JMX_TAG + " tag in AI-Agent.xml is no longer used");
        }

        nodes = builtInElement.getElementsByTagName(MAX_STATEMENT_QUERY_LIMIT_TAG);
        builtInConfigurationBuilder.setQueryPlanThresholdInMS(XmlParserUtils.getLong(XmlParserUtils.getFirst(nodes),
                MAX_STATEMENT_QUERY_LIMIT_TAG));

        agentConfiguration.setBuiltInData(builtInConfigurationBuilder.create());
    }

    private Element getClassDataElement(Node item) {
        if (item.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        Element eClassNode = (Element) item;

        String strValue = eClassNode.getAttribute(ENABLED_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(strValue)) {
            boolean isEnabled = Boolean.valueOf(strValue);
            if (!isEnabled) {
                return null;
            }
        }

        return eClassNode;
    }

    private ClassInstrumentationData getClassInstrumentationData(Element classElement, HashMap<String,
            ClassInstrumentationData> classesToInstrument) {

        String className = classElement.getAttribute(NAME_ATTRIBUTE);
        if (Strings.isNullOrEmpty(className)) {
            return null;
        }

        className = className.replace(".", "/");
        ClassInstrumentationData data = classesToInstrument.get(className);

        String type = classElement.getAttribute("type");
        if (Strings.isNullOrEmpty(type)) {
            type = ClassInstrumentationData.OTHER_TYPE;
        }

        boolean reportCaughtExceptions = false;
        String valueStr = classElement.getAttribute(REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(valueStr)) {
            reportCaughtExceptions = Boolean.valueOf(valueStr);
        }

        boolean reportExecutionTime = true;
        valueStr = classElement.getAttribute(REPORT_EXECUTION_TIME_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(valueStr)) {
            reportExecutionTime = Boolean.valueOf(valueStr);
        }

        long thresholdInMS = 0;
        valueStr = classElement.getAttribute(THRESHOLD_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(valueStr)) {
            thresholdInMS = XmlParserUtils.getLongAttribute(classElement, className, THRESHOLD_ATTRIBUTE, 0);
        }

        if (data == null) {
            data = new ClassInstrumentationData(className, type, reportCaughtExceptions, reportExecutionTime,
                    thresholdInMS);
            classesToInstrument.put(className, data);
        }

        return data;
    }

    private Element getInstrumentationTag(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(INSTRUMENTATION_TAG);
        return XmlParserUtils.getFirst(customTags);
    }

    /**
     * This method is responsible for parsing the Agent Logging Configuration and mimics the configuration
     * style for Core SDK
     */
    private synchronized void initializeAgentLogger(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(AGENT_LOGGER_TAG);
        Element loggerTag = XmlParserUtils.getFirst(customTags);
        if (loggerTag == null) {
            return;
        }

        // Map that stores the configuration for agent logger
        Map<String, String> loggerConfig = new HashMap<>();

        // Get Type Attribute
        String loggerType = loggerTag.getAttribute(SDK_LOGGER_TYPE_TAG);

        customTags = loggerTag.getElementsByTagName(SDK_LOG_LEVEL_TAG);
        Element levelTag = XmlParserUtils.getFirst(customTags);

        if (levelTag != null && !levelTag.getTextContent().trim().isEmpty()) {
            loggerConfig.put(SDK_LOG_LEVEL_TAG, levelTag.getTextContent().trim());
        }

        customTags = loggerTag.getElementsByTagName(SDK_LOGGER_UNIQUE_PREFIX_TAG);
        Element uniquePrefixTag = XmlParserUtils.getFirst(customTags);

        if (uniquePrefixTag != null && !uniquePrefixTag.getTextContent().trim().isEmpty()) {
            loggerConfig.put(SDK_LOGGER_UNIQUE_PREFIX_TAG, uniquePrefixTag.getTextContent().trim());
        }

        customTags = loggerTag.getElementsByTagName(SDK_LOGGER_BASE_FOLDER_PATH_TAG);
        Element baseFolderPathTag = XmlParserUtils.getFirst(customTags);

        if (baseFolderPathTag != null && !baseFolderPathTag.getTextContent().trim().isEmpty()) {
            loggerConfig.put(SDK_LOGGER_BASE_FOLDER_PATH_TAG, baseFolderPathTag.getTextContent().trim());
        }

        customTags = loggerTag.getElementsByTagName(SDK_LOGGER_MAX_NUMBER_OF_LOG_FILES);
        Element maxNoLogFilesTag = XmlParserUtils.getFirst(customTags);

        if (maxNoLogFilesTag != null && !maxNoLogFilesTag.getTextContent().trim().isEmpty()) {
            loggerConfig.put(SDK_LOGGER_MAX_NUMBER_OF_LOG_FILES, maxNoLogFilesTag.getTextContent().trim());
        }

        customTags = loggerTag.getElementsByTagName(SDK_LOGGER_NUMBER_OF_TOTAL_SIZE_IN_MB);
        Element totalLogFileSizeTag = XmlParserUtils.getFirst(customTags);

        if (totalLogFileSizeTag != null && !totalLogFileSizeTag.getTextContent().trim().isEmpty()) {
            loggerConfig.put(SDK_LOGGER_NUMBER_OF_TOTAL_SIZE_IN_MB, totalLogFileSizeTag.getTextContent().trim());
        }

        InternalLogger.INSTANCE.initialize(loggerType, loggerConfig);
    }

    private NodeList getAllClassesToInstrument(Element tag) {
        return tag.getElementsByTagName(CLASS_TAG);
    }

    private void addMethods(ClassInstrumentationData classData, Element eClassNode) {
        NodeList methodNodes = eClassNode.getElementsByTagName(METHOD_TAG);
        if (methodNodes == null || methodNodes.getLength() == 0) {
            if (classData.isReportCaughtExceptions() || classData.isReportExecutionTime()) {
                classData.addAllMethods(classData.isReportCaughtExceptions(), classData.isReportExecutionTime());
            }
            return;
        }

        for (int methodIndex = 0; methodIndex < methodNodes.getLength(); ++methodIndex) {
            Node methodNode = methodNodes.item(methodIndex);
            if (methodNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element methodElement = (Element) methodNode;

            String strValue = methodElement.getAttribute(ENABLED_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(strValue)) {
                boolean isEnabled = Boolean.valueOf(strValue);
                if (!isEnabled) {
                    continue;
                }
            }

            String methodName = methodElement.getAttribute(NAME_ATTRIBUTE);
            if (Strings.isNullOrEmpty(methodName)) {
                continue;
            }

            boolean reportCaughtExceptions = false;
            String valueStr = methodElement.getAttribute(REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(valueStr)) {
                reportCaughtExceptions = Boolean.valueOf(valueStr) || classData.isReportCaughtExceptions();
            }

            boolean reportExecutionTime = true;
            valueStr = methodElement.getAttribute(REPORT_EXECUTION_TIME_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(valueStr)) {
                reportExecutionTime = Boolean.valueOf(valueStr) || classData.isReportExecutionTime();
            }

            if (!reportCaughtExceptions && !reportExecutionTime) {
                continue;
            }

            long thresholdInMS = classData.getThresholdInMS();
            valueStr = methodElement.getAttribute(THRESHOLD_ATTRIBUTE);
            if (!Strings.isNullOrEmpty(valueStr)) {
                try {
                    thresholdInMS = Long.valueOf(valueStr);
                } catch (Exception e) {
                    InternalLogger.INSTANCE.error("Failed to parse attribute '%s' of '%s, default value (true) will " +
                            "be used.'", THRESHOLD_ATTRIBUTE, methodElement.getTagName());
                }
            }

            classData.addMethod(methodName, methodElement.getAttribute(SIGNATURE_ATTRIBUTE), reportCaughtExceptions,
                    reportExecutionTime, thresholdInMS);
        }
    }

    private Element getTopTag(File configurationFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder dBuilder = createDocumentBuilder();
        Document doc = dBuilder.parse(configurationFile);
        doc.getDocumentElement().normalize();

        NodeList topTags = doc.getElementsByTagName(MAIN_TAG);
        if (topTags == null || topTags.getLength() == 0) {
            return null;
        }

        Node topNodeTag = topTags.item(0);
        if (topNodeTag.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        return (Element) topNodeTag;
    }

    private DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        // mitigates CWE-611: https://cwe.mitre.org/data/definitions/611.html
        dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbFactory.setXIncludeAware(false);
        dbFactory.setExpandEntityReferences(false);
        return dbFactory.newDocumentBuilder();
    }
}
