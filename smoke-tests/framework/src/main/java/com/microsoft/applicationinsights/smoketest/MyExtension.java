package com.microsoft.applicationinsights.smoketest;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MyExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {}
}
