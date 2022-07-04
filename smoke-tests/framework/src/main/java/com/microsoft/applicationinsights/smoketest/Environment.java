package com.microsoft.applicationinsights.smoketest;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Environment {

  WarEnvironmentValue value();
}
