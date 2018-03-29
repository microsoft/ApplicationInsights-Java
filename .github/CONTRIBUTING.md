# How to Contribute

If you're interested in contributing, take a look at the general [contributer's guide](https://github.com/Microsoft/ApplicationInsights-Home/blob/master/CONTRIBUTING.md) first.

## Prerequisites

1.  Java SDK 1.7 or higher
2.  Sign-in to [Microsoft Azure](https://azure.com)

## Getting started

1.  Set JAVA_HOME environment variable to point to the JDK installation directory.
2.  To build run `gradlew build`.

### Using Eclipse IDE

1.  Install gradle from http://www.gradle.org/installation
2.  Add GRADLE_HOME/bin to your PATH environment variable
3.  In build.gradle add line [apply plugin: "eclipse"]
4.  In Eclipse used File->Import Existing Project in a workspace.
5.  Use [gradle build] to build the project from the command line.

### CollectD Plugin - Optional

To build Application Insights CollectD writer plugin, please do the following:

1.  Download CollectD Java API sources and compile them using JDK 1.7.
    The output jar should be named: 'collectd-api.jar'.
    More info on compiling CollectD sources can be found here: https://collectd.org/dev-info.shtml
2.  Create a new directory for CollectD library you just created, and set a new environment variable 'COLLECTD_HOME'
    pointing to that folder.   
3.  Copy the new jar into %COLLECTD_HOME%/lib
4.  Reload Application Insights project. CollectD writer plugin sub-project should now be loaded.
    IDE restart may be required in order to identify the new environment variable.

### Notes

* If you are using Java 8+, set JAVA_JRE_7 environment variable to point a Java 7 JRE installation.
* To enable Windows Performance Counters you need to install the [Visual Studio 2013 C++ Redistributable](https://www.microsoft.com/en-us/download/details.aspx?id=40784) (or higher)
