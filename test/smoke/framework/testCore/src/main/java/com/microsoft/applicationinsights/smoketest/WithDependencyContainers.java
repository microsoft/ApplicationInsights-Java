package com.microsoft.applicationinsights.smoketest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicated that the annotated class should also start the listed containers on the same Docker network to be used
 * as test app dependencies. Dependency contianers are started before the test application in the same order listed
 * in this annotation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WithDependencyContainers {
    DependencyContainer[] value();
}
