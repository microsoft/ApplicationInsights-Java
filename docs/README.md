# Application Insights Java Profiler

The Application Insights Java Profiler provides a system for:

1. Generating profiles of the current JVM
2. Monitoring local resource usage and generating a profile if certain conditions are
   met (such as CPU or memory breaching a configured threshold)

# Overview

The Application Insights Java profiler uses the JFR profiler provided by the JVM to record profiling
data. So that users may download the JFR recordings at a later time and analyze them to identify
the cause of performance issues. This data is gathered on demand when trigger conditions are met.
Currently, the available triggers are thresholds over CPU usage and Memory consumption. When a
threshold is breached, a profile of the configured type and duration is gathered and uploaded. This
profile is then visible within the performance blade of the associated Application Insights Portal
UI.

## Warning

The JFR profiler by default executes the "profile-without-env-data" profile. This configuration is
similar to the "profile" configuration that ships with the JVM, however has had some events disabled
that have the potential to contain sensitive deployment information such as environment variables,
arguments provided to the JVM and processes running on the system. The flags that have been disabled
are:

- jdk.JVMInformation
- jdk.InitialSystemProperty
- jdk.OSInformation
- jdk.InitialEnvironmentVariable
- jdk.SystemProcess

However, you should review all flags enabled to ensure that profiles do not contain sensitive data.

If you wish to provide a custom profile configuration, alter the `memoryTriggeredSettings`
and `cpuTriggeredSettings` to provide the path to a `.jfc` file with your required configuration.
Profiles can be generated/edited in the JDK Mission Control (JMC) user
interface under the `Window->Flight Recording Template Manager` menu and control over individual
flags is found inside `Edit->Advanced` of this user interface.

# USAGE

## Installation

1. Inside the `applicationinsights.json` configuration of your process enable the profiler by
   setting the `preview.profiler.enabled` setting:
   ```json
      {
         "connectionString" : "...",
         "preview" : {
            "profiler" : {
               "enabled" : true
            }
         }
      }
   ```
   Alternatively, set the `APPLICATIONINSIGHTS_PROFILER_ENABLED` environment variable to true.
2. Execute your process with the updated configuration.
3. Configure the resource thresholds that will cause a profile to be collected:
    1. Browse to the Performance -> Profiler section of the associated Application Insights instance.
    2. Select "Triggers"
    3. Configure the required CPU and Memory thresholds. And Apply.
    4. Note, currently the Java profiler does not
   support the "Sampling" trigger, configuring this will have no effect.

Once this has been completed, the agent will monitor the resource usage of your process and
trigger a profile when required. Once a profile has been triggered and completed, it will be
viewable from the
Application Insights instance within the Performance -> Profiler section. From that screen the
profile can be downloaded, once download the JFR recording file can be opened and analyzed within a
tool of your choosing.

## Configuration

Configuration of the profiler triggering settings, such as thresholds and profiling periods can be
performed within the ApplicationInsights UI under the Performance, Profiler, Triggers UI as
described in [Installation](#Installation).

Additionally, a number of parameters can be configured using environment variables and the
`applicationinsights.json` configuration file.

### Environment variables

- APPLICATIONINSIGHTS_PROFILER_ENABLED: boolean (default false)
  - Enables/disables the profiling feature.

### Configuration file

```json
{
  "preview": {
    "profiler": {
      "enabled": true,
      "cpuTriggeredSettings": "profile-without-env-data",
      "memoryTriggeredSettings": "profile-without-env-data"
    }
  }
}

```

`memoryTriggeredSettings` - This configuration will be used in the event of a memory profile is
requested. This can be one of:

- `profile-without-env-data` (default value). A profile with certain sensitive events disabled, see
  [Warning](#Warning) section for details.
- `profile`. Uses the `profile.jfc` configuration that ships with JFR.
- A path to a custom jfc configuration file on the file system, i.e `/tmp/myconfig.jfc`.

`cpuTriggeredSettings` - This configuration will be used in the event of a cpu profile is requested.
This can be one of:

- `profile-without-env-data` (default value). A profile with certain sensitive events disabled, see
  [Warning](#Warning) section for details.
- `profile`. Uses the `profile.jfc` jfc configuration that ships with JFR.
- A path to a custom jfc configuration file on the file system, i.e `/tmp/myconfig.jfc`.
