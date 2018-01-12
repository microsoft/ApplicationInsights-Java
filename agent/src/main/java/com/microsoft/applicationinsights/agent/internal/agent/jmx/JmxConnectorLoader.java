/*
 * AppInsights-Java
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

package com.microsoft.applicationinsights.agent.internal.agent.jmx;

import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

import javax.management.*;
import java.lang.management.ManagementFactory;

/**
 * Created by gupele on 8/6/2015.
 */
public final class JmxConnectorLoader {
    private JmxConnectorMXBean mxBean;

    private final static String AI_SDK_JMX_NAME = "com.microsoft.applicationinsights.java.sdk:type=AIJavaSDKAgent";

    public JmxConnectorLoader() {
	}
	
	public boolean initialize() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName(AI_SDK_JMX_NAME);
            mxBean = new JmxConnectorMXBeanImpl();
            mbs.registerMBean(mxBean, name);

            InternalAgentLogger.INSTANCE.info("Successfully registered Jmx connector.");
			
			return true;
        } catch (MalformedObjectNameException e) {
            InternalAgentLogger.INSTANCE.error("Failed to register Jmx connector 'MalformedObjectNameException': '%s'", e.getMessage());
        } catch (NotCompliantMBeanException e) {
            InternalAgentLogger.INSTANCE.error("Failed to register Jmx connector 'NotCompliantMBeanException': '%s'", e.getMessage());
        } catch (InstanceAlreadyExistsException e) {
            InternalAgentLogger.INSTANCE.error("Failed to register Jmx connector 'InstanceAlreadyExistsException': '%s'", e.getMessage());
        } catch (MBeanRegistrationException e) {
            InternalAgentLogger.INSTANCE.error("Failed to register Jmx connector 'MBeanRegistrationException': '%s'", e.getMessage());
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalAgentLogger.INSTANCE.error("Failed to register Jmx connector 'Throwable': '%s'", t.getMessage());
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
		
		return false;
	}
}
