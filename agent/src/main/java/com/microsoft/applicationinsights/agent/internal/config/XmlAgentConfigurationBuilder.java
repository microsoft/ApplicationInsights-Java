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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.common.StringUtils;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Created by gupele on 5/19/2015.
 */
final class XmlAgentConfigurationBuilder implements AgentConfigurationBuilder {
    private final static String AGENT_XML_CONFIGURATION_NAME = "AI-Agent.xml";

    private final static String MAIN_TAG = "ApplicationInsightsAgent";
    private final static String INSTRUMENTATION_TAG = "Instrumentation";
    private final static String CLASS_TAG = "Class";
    private final static String METHOD_TAG = "Method";

    private final static String BUILT_IN_TAG = "BuiltIn";
    private final static String JEDIS_TAG = "Jedis";
    private final static String HTTP_TAG = "HTTP";
    private final static String JDBC_TAG = "JDBC";
    private final static String HIBERNATE_TAG = "HIBERNATE";
    private final static String JMX_TAG = "AgentJmx";
    private final static String MAX_STATEMENT_QUERY_LIMIT_TAG = "MaxStatementQueryLimitInMS";

    private final static String AGENT_LOGGER_TAG = "AgentLogger";

    private final static long JEDIS_ARGS_THRESHOLD_IN_MS = 10000L;

    private final static String EXCLUDED_PREFIXES_TAG = "ExcludedPrefixes";
    private final static String FORBIDDEN_PREFIX_TAG = "Prefix";

    private final static String THRESHOLD_ATTRIBUTE = "thresholdInMS";
    private final static String ENABLED_ATTRIBUTE = "enabled";
    private final static String NAME_ATTRIBUTE = "name";
    private final static String REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE = "reportCaughtExceptions";
    private final static String REPORT_EXECUTION_TIME_ATTRIBUTE = "reportExecutionTime";
    private final static String SIGNATURE_ATTRIBUTE = "signature";

    @Override
    public AgentConfiguration parseConfigurationFile(String baseFolder) {
        AgentConfigurationDefaultImpl agentConfiguration = new AgentConfigurationDefaultImpl();

        String configurationFileName = baseFolder;
        if (!baseFolder.endsWith(File.separator)) {
            configurationFileName += File.separator;
        }
        configurationFileName += AGENT_XML_CONFIGURATION_NAME;

        File configurationFile = new File(configurationFileName);
        if (!configurationFile.exists()) {
            InternalAgentLogger.INSTANCE.trace("Did not find Agent configuration file in '%s'", configurationFileName);
            return agentConfiguration;
        }

        InternalAgentLogger.INSTANCE.trace("Found Agent configuration file in '%s'", configurationFileName);
        try {
            Element topElementTag = getTopTag(configurationFile);
            if (topElementTag == null) {
                return agentConfiguration;
            }

            initializeAgentLogger(topElementTag);

            getForbiddenPaths(topElementTag, agentConfiguration);

            Element instrumentationTag = getInstrumentationTag(topElementTag);
            if (instrumentationTag == null) {
                return agentConfiguration;
            }

            String debugModeAsString = XmlParserUtils.getAttribute(instrumentationTag, "debug");
            if (!StringUtils.isNullOrEmpty(debugModeAsString)) {
                try {
                    boolean debugMode = Boolean.valueOf(debugModeAsString);
                    agentConfiguration.setDebugMode(debugMode);
                    InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Instrumentation debug mode set to '%s'", debugMode);
                } catch (Throwable t) {
                    InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to parse debug attribute '%s'", debugModeAsString);
                }
            }

            setSelfCoreRegistratorMode(agentConfiguration, topElementTag);

            setBuiltInInstrumentation(agentConfiguration, instrumentationTag);

            NodeList addClasses = getAllClassesToInstrument(instrumentationTag);
            if (addClasses == null) {
                return agentConfiguration;
            }

            HashMap<String, ClassInstrumentationData> classesToInstrument = new HashMap<String, ClassInstrumentationData>();
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
                if (data.getMethodInstrumentationInfo().isEmpty() && !data.isReportCaughtExceptions() && !data.isReportExecutionTime()) {
                    classesToInstrument.remove(data.getClassName());
                }
            }

            agentConfiguration.setRequestedClassesToInstrument(classesToInstrument);
            return agentConfiguration;
        } catch (Throwable e) {
            InternalAgentLogger.INSTANCE.error("Exception while parsing Agent configuration file: '%s'",  e.toString());
            return null;
        }
    }

    private void setSelfCoreRegistratorMode(AgentConfigurationDefaultImpl agentConfiguration, Element instrumentationTag) {
        new SelfCoreRegistrationModeBuilder().create(agentConfiguration, instrumentationTag);
    }

    private void getForbiddenPaths(Element parent, AgentConfigurationDefaultImpl agentConfiguration) {
        NodeList nodes = parent.getElementsByTagName(EXCLUDED_PREFIXES_TAG);
        Element forbiddenElement = XmlParserUtils.getFirst(nodes);
        if (forbiddenElement == null) {
            return;
        }

        HashSet<String> excludedPrefixes = new HashSet<String>();

        NodeList addClasses = forbiddenElement.getElementsByTagName(FORBIDDEN_PREFIX_TAG);
        if (addClasses == null) {
            return;
        }

        for (int index = 0; index < addClasses.getLength(); ++index) {
            Element classElement = getClassDataElement(addClasses.item(index));
            if (classElement == null) {
                continue;
            }

            excludedPrefixes.add(classElement.getFirstChild().getTextContent());
        }

        agentConfiguration.setExcludedPrefixes(excludedPrefixes);
    }

    private void setBuiltInInstrumentation(AgentConfigurationDefaultImpl agentConfiguration, Element instrumentationTags) {
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
        long threshold = XmlParserUtils.getLongAttribute(element, JEDIS_TAG, THRESHOLD_ATTRIBUTE, JEDIS_ARGS_THRESHOLD_IN_MS);
        builtInConfigurationBuilder.setJedisValues(XmlParserUtils.getEnabled(element, JEDIS_TAG), threshold);

        new ConfigRuntimeExceptionDataBuilder().setRuntimeExceptionData(builtInElement, builtInConfigurationBuilder);

        nodes = builtInElement.getElementsByTagName(HTTP_TAG);
        builtInConfigurationBuilder.setHttpEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), HTTP_TAG));

        nodes = builtInElement.getElementsByTagName(JDBC_TAG);
        builtInConfigurationBuilder.setJdbcEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), JDBC_TAG));

        nodes = builtInElement.getElementsByTagName(HIBERNATE_TAG);
        builtInConfigurationBuilder.setHibernateEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), HIBERNATE_TAG));

        nodes = builtInElement.getElementsByTagName(JMX_TAG);
        builtInConfigurationBuilder.setJmxEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), JMX_TAG));

        nodes = builtInElement.getElementsByTagName(MAX_STATEMENT_QUERY_LIMIT_TAG);
        builtInConfigurationBuilder.setSqlMaxQueryLimitInMS(XmlParserUtils.getLong(XmlParserUtils.getFirst(nodes), MAX_STATEMENT_QUERY_LIMIT_TAG));

        new BuiltInInstrumentedClassesBuilder().setSimpleBuiltInClasses(builtInConfigurationBuilder, builtInElement);

        agentConfiguration.setBuiltInData(builtInConfigurationBuilder.create());
    }

    private Element getClassDataElement(Node item) {
        if (item.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        Element eClassNode = (Element)item;

        String strValue = eClassNode.getAttribute(ENABLED_ATTRIBUTE);
        if (!StringUtils.isNullOrEmpty(strValue)) {
            boolean isEnabled = Boolean.valueOf(strValue);
            if (!isEnabled) {
                return null;
            }
        }

        return eClassNode;
    }

    private ClassInstrumentationData getClassInstrumentationData(Element classElement, HashMap<String, ClassInstrumentationData> classesToInstrument) {
        String className = classElement.getAttribute(NAME_ATTRIBUTE);
        if (StringUtils.isNullOrEmpty(className)) {
            return null;
        }

        className = className.replace(".", "/");
        ClassInstrumentationData data = classesToInstrument.get(className);

        String type = classElement.getAttribute("type");
        if (StringUtils.isNullOrEmpty(type)) {
            type = InstrumentedClassType.OTHER.toString();
        }

        boolean reportCaughtExceptions = false;
        String valueStr = classElement.getAttribute(REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE);
        if (!StringUtils.isNullOrEmpty(valueStr)) {
            reportCaughtExceptions = Boolean.valueOf(valueStr);
        }

        boolean reportExecutionTime = true;
        valueStr = classElement.getAttribute(REPORT_EXECUTION_TIME_ATTRIBUTE);
        if (!StringUtils.isNullOrEmpty(valueStr)) {
            reportCaughtExceptions = Boolean.valueOf(valueStr);
        }

        long thresholdInMS = 0;
        valueStr = classElement.getAttribute(THRESHOLD_ATTRIBUTE);
        if (!StringUtils.isNullOrEmpty(valueStr)) {
            thresholdInMS = XmlParserUtils.getLongAttribute(classElement, className, THRESHOLD_ATTRIBUTE, 0);
        }

        if (data == null) {
            data = new ClassInstrumentationData(className, type)
                    .setReportExecutionTime(reportExecutionTime)
                    .setReportCaughtExceptions(reportCaughtExceptions)
                    .setThresholdInMS(thresholdInMS);
            classesToInstrument.put(className, data);
        }

        return data;
    }

    private Element getInstrumentationTag(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(INSTRUMENTATION_TAG);
        return XmlParserUtils.getFirst(customTags);
    }

    private void initializeAgentLogger(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(AGENT_LOGGER_TAG);
        Element loggerTag = XmlParserUtils.getFirst(customTags);
        if (loggerTag == null) {
            return;
        }

        InternalAgentLogger.INSTANCE.initialize(loggerTag.getNodeValue());
    }

    private NodeList getAllClassesToInstrument(Element tag) {
        NodeList addClasses = tag.getElementsByTagName(CLASS_TAG);
        if (addClasses == null) {
            return null;
        }
        return addClasses;
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

            Element methodElement = (Element)methodNode;

            String strValue = methodElement.getAttribute(ENABLED_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(strValue)) {
                boolean isEnabled = Boolean.valueOf(strValue);
                if (!isEnabled) {
                    continue;
                }
            }

            String methodName = methodElement.getAttribute(NAME_ATTRIBUTE);
            if (StringUtils.isNullOrEmpty(methodName)) {
                continue;
            }

            boolean reportCaughtExceptions = false;
            String valueStr = methodElement.getAttribute(REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(valueStr)) {
                reportCaughtExceptions = Boolean.valueOf(valueStr) || classData.isReportCaughtExceptions();
            }

            boolean reportExecutionTime = true;
            valueStr = methodElement.getAttribute(REPORT_EXECUTION_TIME_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(valueStr)) {
                reportExecutionTime = Boolean.valueOf(valueStr) || classData.isReportExecutionTime();
            }

            if (!reportCaughtExceptions && !reportExecutionTime) {
                continue;
            }

            long thresholdInMS = classData.getThresholdInMS();
            valueStr = methodElement.getAttribute(THRESHOLD_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(valueStr)) {
                try {
                    thresholdInMS = Long.valueOf(valueStr);
                } catch (Throwable t) {
                    InternalAgentLogger.INSTANCE.error("Failed to parse attribute '%s' of '%s, default value (true) will be used.'", THRESHOLD_ATTRIBUTE, methodElement.getTagName());
                }
            }

            classData.addMethod(methodName, methodElement.getAttribute(SIGNATURE_ATTRIBUTE), reportCaughtExceptions, reportExecutionTime, thresholdInMS);
        }
    }

    public Element getTopTag(File configurationFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();
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

        Element topElementTag = (Element)topNodeTag;
        return topElementTag;
    }
}
