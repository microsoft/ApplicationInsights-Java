# How to Contribute

If you're interested in contributing, take a look at the general [contributer's guide](https://github.com/Microsoft/ApplicationInsights-Home/blob/master/CONTRIBUTING.md) first.

## Prerequisites

1.  Java SDK 1.8 or higher
2.  Sign-in to [Microsoft Azure](https://azure.com)

### Windows Naitive DLLs
* To enable Windows Performance Counters you need to install the [Visual Studio 2013 C++ Redistributable](https://www.microsoft.com/en-us/download/details.aspx?id=40784) (or higher)
* This is skipped automatically on Linux.
* You may skip this part of the build by using the option `-DskipWinNative=true`. Doing so will break support for custom Windows Performance Counters. Do not use this option if you need this functionality.

## Getting started

1.  Set JAVA_HOME environment variable to point to the JDK installation directory.
2.  To build run `gradlew build`.

### Using Eclipse IDE

1.  Install gradle from http://www.gradle.org/installation
2.  Add GRADLE_HOME/bin to your PATH environment variable
3.  In build.gradle add line `apply plugin: 'eclipse'`
4.  In Eclipse used _File -> Import Existing Project_ in a workspace.
5.  Use `gradle build` to build the project from the command line.