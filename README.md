ApplicationInsights-Java
========================
Introduction
------------
Application Insights SDK for Java initially prototyped to be used in Java web services. 

Please refer to [AppInsights-Home](https://github.com/Microsoft/ApplicationInsights-Home) for general application insights documentation.

Prerequisites
-------------
1.  Java SDK 1.6 or higher

Getting started
---------------
1.  Set JAVA_HOME environment variable to point to point to the [JDK installation directory]
2.  Recommended IDE is IntelliJ idea by using ‘Import Project’ rather than ‘Open Project’.
3.  You can use gradlew.bat on Windows to get gradle installed.

Using Eclipse IDE
-----------------
1.  Install gradle from http://www.gradle.org/installation
2.  Add GRADLE_HOME/bin to your PATH environment variable
3.  In build.gradle add line [apply plugin: "eclipse"]
4.  In Eclipse used File->Import Existing Project in a workspace.
5.  Use [gradle build] to build the project from the command line.

CollectD Plugin - Optional
--------------------------
For contributing code to Application Insights CollectD writer plugin, please do the following:
1.  Download CollectD Java API sources and compile them using JDK 1.6 (OpenSDK is preferable).
    The output jar should be named: 'collectd-api.jar'.
    More info on compiling CollectD sources can be found here: https://collectd.org/dev-info.shtml
2.  Create a new directory for CollectD library you just created, and set a new environment variable 'COLLECTD_HOME'
    pointing to that folder.   
3.  Copy the new jar into %COLLECTD_HOME%/lib
4.  Refresh Application Insights project. CollectD writer plugin sub-project should now be loaded.

Notes
-----
* To create a Java 6 compatible build you need to either have JAVA_HOME point to "Java 6 SDK" path or set JAVA_JRE_6 environment variable to point to [JRE 6 JRE installation directory]
