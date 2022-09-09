// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent;

public class Main {

  @SuppressWarnings("SystemOut")
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("No command specified");
      return;
    }
    String command = args[0];
    if (command.equals("unsign")) {
      if (args.length > 1) {
        System.err.println("The unsign command does not expect any arguments");
        return;
      }
      UnsignTool.run();
      return;
    }
    System.err.println("Unknown command: " + command);
  }

  private Main() {}
}
