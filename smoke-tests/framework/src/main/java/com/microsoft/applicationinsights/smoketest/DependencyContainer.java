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

package com.microsoft.applicationinsights.smoketest;

/** Defines a container to be used as a test dependency. */
public @interface DependencyContainer {
  /**
   * The identifier of the docker image. If no {@code imageName} is given, this is used as the image
   * name. If no {@code envionmentVariable} is given, this is used as the environment variable.
   */
  String value();

  /** The name of the docker image. If empty, the {@code value} is used. */
  String imageName() default "";

  String[] environmentVariables() default {};

  String portMapping();

  /**
   * The environment variable used to specify the hostname of this DependencyContainer. If empty,
   * the {@code value} is used as the variable name, in all-caps-snake-case. For example, {@code
   * "myVariable"} becomes {@code "MY_VARIABLE"}.
   */
  String hostnameEnvironmentVariable() default "";
}
