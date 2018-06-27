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

import com.microsoft.applicationinsights.agent.internal.agent.ClassInstrumentationData;
import com.microsoft.applicationinsights.agent.internal.coresync.InstrumentedClassType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Created by gupele on 9/5/2016. */
public class BuiltInInstrumentedClassesBuilder {
  private static final String THRESHOLD_ATTRIBUTE = "Threshold";

  /**
   * The method will go through the classes defined in {@link BuiltInInstrumentedClasses} and will
   * try to add them and the methods that are define for them to the instrumentation classes
   *
   * @param builtInConfigurationBuilder - The builder that will later build the reflection of the
   *     configuration
   * @param builtInElement - The built in element where built in instrumentation is declared
   */
  public static void setSimpleBuiltInClasses(
      AgentBuiltInConfigurationBuilder builtInConfigurationBuilder, Element builtInElement) {
    List<ClassInstrumentationData> classes = new ArrayList<ClassInstrumentationData>();
    Map<String, BuiltInInstrumentedClasses.BuiltInInstrumentedClass> classicBuiltIns =
        new BuiltInInstrumentedClasses().getBuiltInInstrumentedClasses();
    for (Map.Entry<String, BuiltInInstrumentedClasses.BuiltInInstrumentedClass> toInstrument :
        classicBuiltIns.entrySet()) {
      NodeList nodes = builtInElement.getElementsByTagName(toInstrument.getKey());
      boolean isEnabled =
          XmlParserUtils.getEnabled(XmlParserUtils.getFirst(nodes), toInstrument.getKey());
      if (!isEnabled) {
        continue;
      }

      BuiltInInstrumentedClasses.BuiltInInstrumentedClass classPredefinedData =
          toInstrument.getValue();

      Element builtInTagElement = XmlParserUtils.getFirst(nodes);
      long threshold =
          XmlParserUtils.getLongAttribute(
              builtInTagElement,
              toInstrument.getKey(),
              THRESHOLD_ATTRIBUTE,
              classPredefinedData.getThresholdInMS());
      ClassInstrumentationData data =
          new ClassInstrumentationData(
              classPredefinedData.getClassName(), InstrumentedClassType.OTHER);
      data.setThresholdInMS(threshold);

      List<String> methods = classPredefinedData.getMethods();
      if (methods == null) {
        data.addAllMethods(false, true);
      } else {
        for (String method : methods) {
          data.addMethod(method, "", false, true, threshold);
        }
      }

      classes.add(data);
    }
    builtInConfigurationBuilder.setSimpleBuiltInClasses(classes);
  }
}
