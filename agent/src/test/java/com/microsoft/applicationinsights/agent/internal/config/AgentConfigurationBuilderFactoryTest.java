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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AgentConfigurationBuilderFactoryTest {
  private static void verifyXmlBuilder(AgentConfigurationBuilder builder) {
    assertNotNull(builder);
    assertTrue(builder instanceof XmlAgentConfigurationBuilder);
  }

  @Test
  public void testCreateDefaultBuilder() {
    AgentConfigurationBuilder builder =
        new AgentConfigurationBuilderFactory().createDefaultBuilder();
    verifyXmlBuilder(builder);
  }

  @Test
  public void testCreateXmlBuilder() {
    AgentConfigurationBuilder builder =
        new AgentConfigurationBuilderFactory()
            .createBuilder(
                "com.microsoft.applicationinsights.agent.internal.config.XmlAgentConfigurationBuilder");
    verifyXmlBuilder(builder);
  }

  @Test
  public void testCreateNullBuilder() {
    AgentConfigurationBuilder builder = new AgentConfigurationBuilderFactory().createBuilder(null);
    verifyXmlBuilder(builder);
  }

  @Test
  public void testCreateBadBuilder() {
    AgentConfigurationBuilder builder =
        new AgentConfigurationBuilderFactory().createBuilder("java/lang/Object");
    assertNull(builder);
  }
}
