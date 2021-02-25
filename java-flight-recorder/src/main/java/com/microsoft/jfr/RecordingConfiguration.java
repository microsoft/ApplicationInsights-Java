package com.microsoft.jfr;

import java.util.Objects;

/**
 * A flight recorder configuration controls the amount of data that is collected.
 * At this time, only pre-defined configurations are supported.
 * A pre-defined configuration is one which you could select with the 'settings' option
 * of the JVM option 'StartFlightRecording', for example {@code -XX:StartFlightRecording:settings=default.jfc}.
 */
public class RecordingConfiguration {

    /**
     * Convenience for selecting the pre-defined 'default' configuration that is standard with the JDK.
     * The default configuration is suitable for continuous recordings.
     */
    public static final RecordingConfiguration DEFAULT_CONFIGURATION = new RecordingConfiguration("default");

    /**
     * Convenience for referencing the 'profile' configuration that is standard with the JDK.
     * The profile configuration collects more events and is suitable for profiling an application.
     */
    public static final RecordingConfiguration PROFILE_CONFIGURATION = new RecordingConfiguration("profile");

    /**
     * Sets a pre-defined configuration to use with a {@code Recording}.
     * @param configurationName The name of the pre-defined configuration, not {@code null}.
     * @throws NullPointerException if predefinedConfiguration is {@code null}
     */
    public RecordingConfiguration(String configurationName) {
        Objects.requireNonNull(configurationName, "configurationName cannot be null");
        this.name = configurationName;
    }

    /**
     * Get the name of a pre-defined configuration. The return value will be {@code null}
     * if a pre-defined configuration has not been set.
     * @return The name of a pre-defined configuration, or {@code null}.
     */
    public String getName() {
        return name;
    }

    // A configuration name, such as "default" or "profile"
    private final String name;

}
