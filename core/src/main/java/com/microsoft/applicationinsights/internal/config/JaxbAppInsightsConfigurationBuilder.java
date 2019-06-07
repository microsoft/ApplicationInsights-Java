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

package com.microsoft.applicationinsights.internal.config;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * The JAXB implementation of the {@link com.microsoft.applicationinsights.internal.config.AppInsightsConfigurationBuilder}
 *
 * Created by gupele on 3/15/2015.
 */
class JaxbAppInsightsConfigurationBuilder implements AppInsightsConfigurationBuilder {
    @Override
    public ApplicationInsightsXmlConfiguration build(InputStream resourceFile) {
        if (resourceFile == null) {
            return null;
        }

        Thread currentThread = Thread.currentThread();
        ClassLoader priorContextClassLoader = currentThread.getContextClassLoader();
        try {
            // when the agent is running and this class is in the bootstrap class loader, we need to load JAXB also
            // from the bootstrap class loader, otherwise when thread context class loader is the restrictive JBoss
            // Modules Class Loader, it will throw ClassNotFoundException: com.sun.xml.internal.bind.v2.ContextFactory
            ClassLoader loader = JaxbAppInsightsConfigurationBuilder.class.getClassLoader();
            if (loader == null) {
                loader = ClassLoader.getSystemClassLoader();
            }
            currentThread.setContextClassLoader(loader);
            XStream xstream = new XStream(new PureJavaReflectionProvider(), new StaxDriver());

            xstream.ignoreUnknownElements(); // backwards compatible with jaxb behavior

            XStream.setupDefaultSecurity(xstream);
            xstream.allowTypesByWildcard(new String[] {
                    "com.microsoft.applicationinsights.internal.config.*"
            });
            xstream.processAnnotations(ApplicationInsightsXmlConfiguration.class);

            ApplicationInsightsXmlConfiguration applicationInsights =
                    (ApplicationInsightsXmlConfiguration) xstream.fromXML(resourceFile);

            return applicationInsights;
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to parse configuration file: '%s'", ExceptionUtils.getStackTrace(e));
            // write directly to STDOUT, since config failed to load and so internal logger may not be configured
            e.printStackTrace();
        } finally {
            currentThread.setContextClassLoader(priorContextClassLoader);
            try {
                resourceFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Given an InputStream, returns an XMLStreamReader for it. Explicitly disables DTDs and external entities.
     */
    private XMLStreamReader getXmlStreamReader(InputStream input) {

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            
            return factory.createXMLStreamReader(input);
        } catch (Throwable t ) {
            InternalLogger.INSTANCE.error("Failed to create stream reader for configuration file: '%s'", ExceptionUtils.getStackTrace(t));
            return null;
        }
    } 
}

