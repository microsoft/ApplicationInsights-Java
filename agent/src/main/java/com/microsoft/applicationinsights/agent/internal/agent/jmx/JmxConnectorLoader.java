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

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import java.lang.management.ManagementFactory;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.lang3.exception.ExceptionUtils;

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

            InternalLogger.INSTANCE.info("Successfully registered Jmx connector.");
			
			return true;
        } catch (MalformedObjectNameException e) {
            InternalLogger.INSTANCE.error("Failed to register Jmx connector 'MalformedObjectNameException': '%s'",
                ExceptionUtils.getStackTrace(e));
        } catch (NotCompliantMBeanException e) {
            InternalLogger.INSTANCE.error("Failed to register Jmx connector 'NotCompliantMBeanException': '%s'",
                ExceptionUtils.getStackTrace(e));
        } catch (InstanceAlreadyExistsException e) {
            InternalLogger.INSTANCE.error("Failed to register Jmx connector 'InstanceAlreadyExistsException': '%s'",
                ExceptionUtils.getStackTrace(e));
        } catch (MBeanRegistrationException e) {
            InternalLogger.INSTANCE.error("Failed to register Jmx connector 'MBeanRegistrationException': '%s'",
                ExceptionUtils.getStackTrace(e));
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.error("Failed to register Jmx connector 'Throwable': '%s'", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
		
		return false;
	}
}
