package com.microsoft.applicationinsights.smoketest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that this test class should use the agent and selects the configuration file for the agent to use.
 *
 * <p>The configuration files should be placed in {@code /test/smoke/appServers/global-resources/{mode}_AI-Agent.xml}.</p>
 *
 * <p>
 *     The {mode} prefix differentiates the configurations for various purposes and the mode is selected by the
 *     {@link #value()} in this annotation. For example, a configuration file named {@code myconfig_AI-Agent.xml}
 *     can be selected by annotating a class with {@code @UseAgent("myconfig")}.
 * </p>
 *
 * <p>
 *     When the configuration file is used, the selected configuration file is copied to the agent library's directory
 *     and renamed to {@code AI-Agent.xml}.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UseAgent {
    /**
     * Sets the agent mode, i.e. chooses the config file to use.
     */
    String value() default "default";
}
