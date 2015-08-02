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

package com.microsoft.applicationinsights.agent.internal.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import com.microsoft.applicationinsights.agent.internal.config.AgentConfiguration;
import com.microsoft.applicationinsights.agent.internal.config.AgentConfigurationBuilderFactory;
import com.microsoft.applicationinsights.agent.internal.logger.InternalAgentLogger;

/**
 * The class is responsible for finding needed classes
 *
 * Created by gupele on 5/11/2015.
 */
public final class CodeInjector implements ClassFileTransformer {

    private final ClassDataProvider classNamesProvider = new DefaultClassDataProvider();

    /**
     * The constructor will set all the data needed for the transformation and then
     * will register itself as a {@link ClassFileTransformer} that will be called to manipulate the byte code
     *
     * @param inst             The instrumentation instance that we use to register the CodeInjector
     * @param agentJarLocation The location of the agent jar
     */
    public CodeInjector(Instrumentation inst, String agentJarLocation) {
        try {
            loadConfiguration(agentJarLocation);

            inst.addTransformer(this);

            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.INFO, "Agent is up");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.INFO, "Agent is NOT activated: failed to initialize CodeInjector: '%s'", throwable.getMessage());
        }
    }

    /**
     * Main method that transforms classes
     * @param loader The class loader that loaded this class
     * @param className The class name
     * @param classBeingRedefined The class that is being redefined
     * @param protectionDomain The protection domain
     * @param originalBuffer The class that was loaded before transforming it
     * @return A byte array that contains the transformed original class or the original one if nothing was done.
     * @throws IllegalClassFormatException Theoretical, since the following implementation won't throw.
     */
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] originalBuffer) throws IllegalClassFormatException {

        ByteCodeTransformer byteCodeTransformer = classNamesProvider.getAndRemove(className);
        if (byteCodeTransformer != null) {
            try {
                return byteCodeTransformer.transform(originalBuffer);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                InternalAgentLogger.INSTANCE.logAlways(InternalAgentLogger.LoggingLevel.ERROR, "Failed to instrument '%s', exception: '%s': ", className, throwable.getMessage());
            }
        }

        return originalBuffer;
    }

    /**
     * The method will try to load the configuration file for the Agent. The file is optional but
     * is assumed to be located 'near' the agent jar. Failing to put the file there will cause the file not to be loaded
     * @param agentJarLocation The agent jar location
     */
    private void loadConfiguration(String agentJarLocation) {
        AgentConfiguration agentConfiguration = new AgentConfigurationBuilderFactory().createDefaultBuilder().parseConfigurationFile(agentJarLocation);
        classNamesProvider.setConfiguration(agentConfiguration);
    }
}
