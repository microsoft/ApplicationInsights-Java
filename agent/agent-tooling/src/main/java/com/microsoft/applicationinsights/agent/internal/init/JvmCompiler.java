// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.internal.init;

import com.microsoft.applicationinsights.agent.bootstrap.diagnostics.PidFinder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class JvmCompiler {

  // To disable C2 compiler during Application Insights set-up, for Java >= 9, do the following
  // things.
  // Execute with -XX:+UnlockDiagnosticVMOptions -XX:CompilerDirectivesFile=path/jvm-disable-c2.json
  // Content of jvm-disable-c2.json file:
  // [
  //  {
  //    match: [
  //      "*::*"
  //    ],
  //    c2: {
  //      Exclude: true
  //    }
  //  }
  // ]
  private static final String
      APPLICATIONINSIGHTS_EXPERIMENT_CLEAR_COMPILER_DIRECTIVES_AFTER_INITIALIZATION =
          "applicationinsights.experiment.clear-compiler-directives-after-initialization";

  private JvmCompiler() {}

  static boolean hasToDisableJvmCompilerDirectives() {
    return Boolean.getBoolean(
        APPLICATIONINSIGHTS_EXPERIMENT_CLEAR_COMPILER_DIRECTIVES_AFTER_INITIALIZATION);
  }

  @SuppressFBWarnings(
      value = "SECCI", // Command Injection
      justification = "No user data is used to construct the command below")
  static void disableJvmCompilerDirectives() {
    CommandExecutor.execute(new ProcessBuilder("jcmd", pid(), "Compiler.directives_clear"));
  }

  private static String pid() {
    return new PidFinder().getValue();
  }
}
