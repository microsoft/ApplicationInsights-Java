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
package com.microsoft.applicationinsights.autoconfigure.conditionals;

import com.microsoft.applicationinsights.autoconfigure.helpers.IkeyResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Conditional to check if instrumentation key is either specified using
 * 1. azure.application-insights.instrumentation-key
 * 2. APPLICATION_INSIGHTS_IKEY
 * 3. APPINSIGHTS_INSTRUMENTATIONKEY
 *
 * @author Dhaval Doshi
 */
public class InstrumentationKeyCondition extends SpringBootCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String iKey = context.getEnvironment().getProperty("azure.application-insights.instrumentation-key");
    if (StringUtils.isNoneBlank(iKey)) {
      return new ConditionOutcome(true, ConditionMessage.of("instrumentation key found"));
    }
    iKey = IkeyResolver.getIkeyFromEnvironmentVariables();
    if (StringUtils.isNoneBlank(iKey)) {
      return new ConditionOutcome(true, ConditionMessage.of("instrumentation key found"));
    }
    else {
      return new ConditionOutcome(false, ConditionMessage.of("instrumentation key not found"));
    }
  }
}
