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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Strings;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.ClassInstrumentationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XmlAgentConfigurationBuilder {

    private static final Logger logger = LoggerFactory.getLogger(XmlAgentConfigurationBuilder.class);

    private static final String AGENT_XML_CONFIGURATION_NAME = "AI-Agent.xml";

    private static final String MAIN_TAG = "ApplicationInsightsAgent";
    private static final String INSTRUMENTATION_TAG = "Instrumentation";
    private static final String CLASS_TAG = "Class";
    private static final String METHOD_TAG = "Method";

    private static final String BUILT_IN_TAG = "BuiltIn";
    private static final String JEDIS_TAG = "Jedis";
    private static final String HTTP_TAG = "HTTP";
    private static final String JDBC_TAG = "JDBC";
    private static final String LOGGING_TAG = "Logging";
    private static final String JMX_TAG = "AgentJmx";
    private static final String MAX_STATEMENT_QUERY_LIMIT_TAG = "MaxStatementQueryLimitInMS";

    private static final String W3C_ENABLED = "W3C";
    private static final String W3C_BACKCOMPAT_PARAMETER = "enableW3CBackCompat";

    private static final String EXCLUDED_PREFIXES_TAG = "ExcludedPrefixes";

    private static final String RUNTIME_EXCEPTION_TAG = "RuntimeException";

    private static final String THRESHOLD_ATTRIBUTE = "thresholdInMS";
    private static final String ENABLED_ATTRIBUTE = "enabled";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE = "reportCaughtExceptions";
    private static final String REPORT_EXECUTION_TIME_ATTRIBUTE = "reportExecutionTime";
    private static final String SIGNATURE_ATTRIBUTE = "signature";

    public AgentConfiguration parseConfigurationFile(String baseFolder) {
        AgentConfiguration agentConfiguration = new AgentConfiguration();

        String configurationFileName = baseFolder;
        if (!baseFolder.endsWith(File.separator)) {
            configurationFileName += File.separator;
        }
        configurationFileName += AGENT_XML_CONFIGURATION_NAME;

        File configurationFile = new File(configurationFileName);
        if (!configurationFile.exists()) {
            logger.trace("Did not find Agent configuration file in '{}'", configurationFileName);
            return agentConfiguration;
        }

        logger.trace("Found Agent configuration file in '{}'", configurationFileName);
        try {
            Element topElementTag = getTopTag(configurationFile);
            if (topElementTag == null) {
                return agentConfiguration;
            }

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
                logger.error("Exception while parsing Agent configuration file: '{}'", e.toString());
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
            logger.warn("{} tag in AI-Agent.xml is no longer used", EXCLUDED_PREFIXES_TAG);
        }
    }

    private void setBuiltInInstrumentation(AgentConfiguration agentConfiguration,
                                           Element instrumentationTags) {
        BuiltInInstrumentationBuilder builtInConfigurationBuilder = new BuiltInInstrumentationBuilder();

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
            logger.warn("{} tag in AI-Agent.xml is no longer used", RUNTIME_EXCEPTION_TAG);
        }

        nodes = builtInElement.getElementsByTagName(HTTP_TAG);
        Element httpElement = XmlParserUtils.getFirst(nodes);
        boolean isW3CEnabled = XmlParserUtils.w3cEnabled(httpElement, W3C_ENABLED, false);
        boolean isW3CBackportEnabled = XmlParserUtils.w3cEnabled(httpElement, W3C_BACKCOMPAT_PARAMETER, true);
        builtInConfigurationBuilder.setHttpEnabled(XmlParserUtils.getEnabled(element, HTTP_TAG), isW3CEnabled,
                isW3CBackportEnabled);

        nodes = builtInElement.getElementsByTagName(JDBC_TAG);
        builtInConfigurationBuilder.setJdbcEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), JDBC_TAG));

        nodes = builtInElement.getElementsByTagName(LOGGING_TAG);
        builtInConfigurationBuilder
                .setLoggingEnabled(XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), LOGGING_TAG));

        nodes = builtInElement.getElementsByTagName(JMX_TAG);
        Element jmxElement = XmlParserUtils.getFirst(nodes);
        if (jmxElement != null) {
            logger.warn("{} tag in AI-Agent.xml is no longer used", JMX_TAG);
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

        Element classNode = (Element) item;

        String strValue = classNode.getAttribute(ENABLED_ATTRIBUTE);
        if (!Strings.isNullOrEmpty(strValue)) {
            boolean isEnabled = Boolean.valueOf(strValue);
            if (!isEnabled) {
                return null;
            }
        }

        return classNode;
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

    private NodeList getAllClassesToInstrument(Element tag) {
        return tag.getElementsByTagName(CLASS_TAG);
    }

    private void addMethods(ClassInstrumentationData classData, Element classNode) {
        NodeList methodNodes = classNode.getElementsByTagName(METHOD_TAG);
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
                    logger.error("Failed to parse attribute '{}' of '{}', default value (true) will be used.'",
                            THRESHOLD_ATTRIBUTE, methodElement.getTagName());
                }
            }

            classData.addMethod(methodName, methodElement.getAttribute(SIGNATURE_ATTRIBUTE), reportCaughtExceptions,
                    reportExecutionTime, thresholdInMS);
        }
    }

    private Element getTopTag(File configurationFile) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = createDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(configurationFile));
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
