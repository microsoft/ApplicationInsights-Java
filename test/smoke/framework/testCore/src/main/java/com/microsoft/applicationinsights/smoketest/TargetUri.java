package com.microsoft.applicationinsights.smoketest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TargetUri {
    String value();
    String method() default "GET";

    /**
     * The delay in milliseconds to wait before calling the target uri.
     */
    long delay() default 0L;
}
