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

package com.microsoft.applicationinsights.smoketest.fixtures;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.junit.runners.parameterized.TestWithParameters;

public class ParameterizedRunnerWithFixtures extends BlockJUnit4ClassRunnerWithParameters {
  private final Object[] parameters;

  public ParameterizedRunnerWithFixtures(TestWithParameters twp) throws InitializationError {
    super(twp);
    this.parameters = twp.getParameters().toArray(new Object[twp.getParameters().size()]);
  }

  private Statement wrapWithParamBefores(Statement s) {
    List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(BeforeWithParams.class);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        for (FrameworkMethod fm : methods) {
          fm.invokeExplosively(null, ParameterizedRunnerWithFixtures.this.parameters);
        }
        s.evaluate();
      }
    };
  }

  private Statement wrapWithParamAfters(Statement s) {
    List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(AfterWithParams.class);
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        List<Throwable> errs = new ArrayList<>();
        try {
          s.evaluate();
        } catch (Throwable t) {
          errs.add(t);
        } finally {
          for (FrameworkMethod fm : methods) {
            try {
              fm.invokeExplosively(null, ParameterizedRunnerWithFixtures.this.parameters);
            } catch (Throwable t) {
              errs.add(t);
            }
          }
        }
        MultipleFailureException.assertEmpty(errs);
      }
    };
  }

  @Override
  protected Statement classBlock(RunNotifier notifier) {
    return wrapWithParamAfters(wrapWithParamBefores(super.classBlock(notifier)));
  }

  @Override
  protected void collectInitializationErrors(List<Throwable> errors) {
    super.collectInitializationErrors(errors);
    validateFixtures(errors);
  }

  private void validateFixtures(List<Throwable> errors) {
    validateFixture(BeforeWithParams.class, errors);
    validateFixture(AfterWithParams.class, errors);
  }

  private void validateFixture(Class<? extends Annotation> annotation, List<Throwable> errors) {
    List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(annotation);
    for (FrameworkMethod fm : methods) {
      fm.validatePublicVoid(true, errors);
    }
  }
}
