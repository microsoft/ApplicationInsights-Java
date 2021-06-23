package com.microsoft.applicationinsights.smoketest;

/**
 * Defines a container to be used as a test dependency.
 */
public @interface DependencyContainer {
    /**
     * The identifier of the docker image.
     * If no {@code imageName} is given, this is used as the image name.
     * If no {@code envionmentVariable} is given, this is used as the environment variable.
     */
    String value();

    /**
     * The name of the docker image.
     * If empty, the {@code value} is used.
     */
    String imageName() default "";


    String[] environmentVariables() default {};

    String portMapping();

    /**
     * The environment variable used to specify the hostname of this DependencyContainer.
     * If empty, the {@code value} is used as the variable name, in all-caps-snake-case.
     * For example, {@code "myVariable"} becomes {@code "MY_VARIABLE"}.
     */
    String hostnameEnvironmentVariable() default "";
}
