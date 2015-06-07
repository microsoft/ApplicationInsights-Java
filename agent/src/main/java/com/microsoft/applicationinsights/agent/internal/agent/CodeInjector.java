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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * The class is responsible for finding needed classes
 *
 * Created by gupele on 5/11/2015.
 */
public final class CodeInjector implements ClassFileTransformer {

    private final ClassNamesProvider classNamesProvider = new DefaultClassNamesProvider();
    private MethodWrapperFactory factory;

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
            factory = new MethodWrapperFactory(classNamesProvider);

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

        ClassInstrumentationData classInstrumentationData = classNamesProvider.getAndRemove(className);
        if (classInstrumentationData != null) {
            try {
                return getTransformedBytes(originalBuffer, classInstrumentationData);
            } catch (Throwable throwable) {
                System.err.println(String.format("Failed to instrument '%s', exception: '%s': ", classInstrumentationData, throwable.getMessage()));
            }
        }

        return originalBuffer;
    }

    /**
     * The method will create the {@link EnterExitClassVisitor}
     * which is responsible for injecting the code to do so the method will pass the class the needed data that will enable its work
     * @param originalBuffer The original buffer of the class
     * @param instrumentationData The instrumentation data for that class
     * @return The transformed byte array if there was a change, otherwise the original one
     */
    private byte[] getTransformedBytes(byte[] originalBuffer, ClassInstrumentationData instrumentationData) {
        ClassReader cr = new ClassReader(originalBuffer);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        EnterExitClassVisitor mcw = new EnterExitClassVisitor(factory, instrumentationData, cw);
        cr.accept(mcw, ClassReader.EXPAND_FRAMES);
        byte[] b2 = cw.toByteArray();
        return b2;
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
