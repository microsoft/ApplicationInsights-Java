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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.agent.StringUtils;
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
    private final static String HTTP_TAG = "HTTP";
    private final static String JDBC_TAG = "JDBC";
    private final static String HIBERNATE_TAG = "HIBERNATE";

    private final static String AGENT_LOGGER_TAG = "AgentLogger";

    private final static String ENABLED_ATTRIBUTE = "enabled";
    private final static String NAME_ATTRIBUTE = "name";
    private final static String REPORT_CAUGHT_EXCEPTIONS_ATTRIBUTE = "reportCaughtExceptions";
    private final static String REPORT_EXECUTION_TIME_ATTRIBUTE = "reportExecutionTime";
    private final static String SIGNATURE_ATTRIBUTE = "signature";

    @Override
    public AgentConfiguration parseConfigurationFile(String baseFolder) {
        XmlAgentConfiguration agentConfiguration = new XmlAgentConfiguration();

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



            Element instrumentationTag = getInstrumentationTag(topElementTag);
            if (instrumentationTag == null) {
                return agentConfiguration;
            }

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
                if (data.methodInstrumentationInfo.isEmpty() && !data.reportCaughtExceptions && !data.reportExecutionTime) {
                    classesToInstrument.remove(data.className);
                }
            }

            agentConfiguration.setRequestedClassesToInstrument(classesToInstrument);
            return agentConfiguration;
        } catch (Throwable e) {
            InternalAgentLogger.INSTANCE.error("Exception while parsing Agent configuration file: '%s'" + e.getMessage());
            return null;
        }
    }

    private void setBuiltInInstrumentation(XmlAgentConfiguration agentConfiguration, Element instrumentationTags) {
        AgentBuiltInConfigurationBuilder builtInConfigurationBuilder = new AgentBuiltInConfigurationBuilder();

        NodeList nodes = instrumentationTags.getElementsByTagName(BUILT_IN_TAG);
        Element builtInElement = getFirst(nodes);
        if (builtInElement == null) {
            agentConfiguration.setBuiltInData(builtInConfigurationBuilder.create());
            return;
        }

        builtInConfigurationBuilder.setEnabled(getEnabled(builtInElement, BUILT_IN_TAG));

        nodes = builtInElement.getElementsByTagName(HTTP_TAG);
        builtInConfigurationBuilder.setHttpEnabled(getEnabled(getFirst(nodes), HTTP_TAG));

        nodes = builtInElement.getElementsByTagName(JDBC_TAG);
        builtInConfigurationBuilder.setJdbcEnabled(getEnabled(getFirst(nodes), JDBC_TAG));

        nodes = builtInElement.getElementsByTagName(HIBERNATE_TAG);
        builtInConfigurationBuilder.setHibernateEnabled(getEnabled(getFirst(nodes), HIBERNATE_TAG));

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

        InstrumentedClassType type = InstrumentedClassType.OTHER;
        try {
            type = Enum.valueOf(InstrumentedClassType.class, classElement.getAttribute("type"));
        } catch (Throwable t) {
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

        if (data == null) {
            data = new ClassInstrumentationData(className, type)
                    .setReportExecutionTime(reportExecutionTime)
                    .setReportCaughtExceptions(reportCaughtExceptions);
            classesToInstrument.put(className, data);
        }

        return data;
    }

    private Element getInstrumentationTag(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(INSTRUMENTATION_TAG);
        return getFirst(customTags);
    }

    private void initializeAgentLogger(Element topElementTag) {
        NodeList customTags = topElementTag.getElementsByTagName(AGENT_LOGGER_TAG);
        Element loggerTag = getFirst(customTags);
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
            if (classData.reportCaughtExceptions || classData.reportExecutionTime) {
                classData.addAllMethods(classData.reportCaughtExceptions, classData.reportExecutionTime);
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
                reportCaughtExceptions = Boolean.valueOf(valueStr) || classData.reportCaughtExceptions;
            }

            boolean reportExecutionTime = true;
            valueStr = methodElement.getAttribute(REPORT_EXECUTION_TIME_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(valueStr)) {
                reportExecutionTime = Boolean.valueOf(valueStr) || classData.reportExecutionTime;
            }

            if (!reportCaughtExceptions && !reportExecutionTime) {
                continue;
            }

            classData.addMethod(methodName, methodElement.getAttribute(SIGNATURE_ATTRIBUTE), reportCaughtExceptions, reportExecutionTime);
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

    private Element getFirst(NodeList nodes) {
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }

        Node node = nodes.item(0);
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return null;
        }

        return (Element)node;
    }

    private boolean getEnabled(Element element, String elementName) {
        if (element == null) {
            return true;
        }

        try {
            String strValue = element.getAttribute(ENABLED_ATTRIBUTE);
            if (!StringUtils.isNullOrEmpty(strValue)) {
                boolean value = Boolean.valueOf(strValue);
                return value;
            }
            return true;
        } catch (Throwable t) {
            InternalAgentLogger.INSTANCE.error("Failed to parse attribute '%s' of '%s, default value (true) will be used.'", ENABLED_ATTRIBUTE, elementName);
        }

        return false;
    }
}
