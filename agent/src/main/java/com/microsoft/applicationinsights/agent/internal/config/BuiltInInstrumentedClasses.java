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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This class lets us add classes that will be a part of the 'BuiltIn' section in the AI-Agent.xml.
 *
 * This BuiltIn secion declares the classes and methods that the agent will instrument automatically.
 *
 * To add a new class you need to declare the XML tag for that class, which can be used by the user to
 * either disable it, or change the threshold in which telemetries are sent for its instrumented methods.
 *
 * To add a new class you can do one of the following:
 *
 * {@code
 *   // Start define new class
 *   builtInInstrumentedClasses.put(
 *    // The XML tag in AI-Agent.xml (in the 'BuiltIn' section)
 *       "NewClassLogicalName",
 *
 *    new BuiltInInstrumentedClass(
 *       // Full class name
 *       "an.example.of.full.class.path.to.RelevantClass",
 *
 *     // All methods to instrument
 *      "method1", "method2"));
 *
 * }
 *
 * or (no method define, all methods will be instrumented)
 * {@code
 *   // Start define new class
 *   builtInInstrumentedClasses.put(
 *   // The XML tag in AI-Agent.xml (in the 'BuiltIn' section)
 *   "NewClassLogicalName",
 *
 *   new BuiltInInstrumentedClass(
 *       // Full class name
 *       "an.example.of.full.class.path.to.RelevantClass"));
 *
 * }
 *
 * or (set threshold in MS. Methods that will pass this threshold will be reported)
 *   {@code
 *   // Start define new class
 *   builtInInstrumentedClasses.put(
 *   // The XML tag in AI-Agent.xml (in the 'BuiltIn' section)
 *   "NewClassLogicalName",
 *
 *  new BuiltInInstrumentedClass(
 *  // Full class name
 *  "an.example.of.full.class.path.to.RelevantClass", 100L));
 *
 * }
 *
 * To disable the class from being instrumented you now need to go to the AI-Agent.xml and:
 *
 * {@code
 *      <BuiltIn>
 *
 *           <NewClassLogicalName enabled="false"/>
 *
 *       </BuiltIn>
 * }
 *
 * To change the class threshold:
 *
 * {@code
 *    <BuiltIn>
 *
 *       <NewClassLogicalName thresholdInMS="10001"/>
 *
 *   </BuiltIn>
 * }
 *
 * Created by gupele on 8/2/2016.
 */
final class BuiltInInstrumentedClasses {

    public static class BuiltInInstrumentedClass {
        private final String className;
        private final long thresholdInMS;
        private ArrayList<String> methods;

        public BuiltInInstrumentedClass(String className) {
            this(className, 0, (String[])null);
        }

        public BuiltInInstrumentedClass(String className, long thresholdInMS) {
            this(className, thresholdInMS, (String[])null);
        }

        public BuiltInInstrumentedClass(String className, String... methods) {
            this(className, 0, methods);
        }

        public BuiltInInstrumentedClass(String className, long thresholdInMS, String... methods) {
            this.className = className.replace(".", "/");
            this.thresholdInMS = thresholdInMS;
            this.methods = new ArrayList<String>();
            if (methods != null) {
                for (String method : methods){
                    this.methods.add(method);
                }
            }
        }

        public String getClassName() {
            return className;
        }

        public List<String> getMethods() {
            return methods;
        }

        public long getThresholdInMS() {
            return thresholdInMS;
        }
    }

    // Add the needed built in classes and methods
    private static final Map<String, BuiltInInstrumentedClass> builtInInstrumentedClasses;
    static
    {
        builtInInstrumentedClasses = new HashMap<String, BuiltInInstrumentedClass>();

        // Mule ESB 3.3.0
        builtInInstrumentedClasses.put(
                // The XML tag in AI-Agent.xml (in the 'BuiltIn' section)
                "MuleESB",

                new BuiltInInstrumentedClass(
                        // Full class name
                        "org.mule.module.client.MuleClient",

                        // All methods to instrument
                        "dispatch", "send", "sendAsync", "sendDirect", "sendDirectAsync", "request"));
    }

    public Map<String, BuiltInInstrumentedClass> getBuiltInInstrumentedClasses() {
        return builtInInstrumentedClasses;
    }
}
