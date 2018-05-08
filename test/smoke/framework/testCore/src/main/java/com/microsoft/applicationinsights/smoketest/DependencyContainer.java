package com.microsoft.applicationinsights.smoketest;

public @interface DependencyContainer {
    /**
     * The identifier of the docker image.
     * If no {@code imageName} is given, this is used as the image name.
     * If no {@code envionmentVariable} is given, this is used as the environment variable.
     * @return
     */
    String value();

    /**
     * The name of the docker image.
     * If empty, the {@code value} is used.
     * @return
     */
    String imageName() default "";


    /**
     * The environment variable used to specify the hostname of this DependencyContainer.
     * If empty, the {@code value} is used as the variable name, in all-caps-snake-case.
     * For example, {@code "myVariable"} becomes {@code "MY_VARIABLE"}.
     * @return
     */
    String environmentVariable() default "";
}
