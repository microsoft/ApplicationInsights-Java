AppInsights-Java
================
Introduction
------------
Applicaiton Insights SDK for Java initially prototyped to be used in java web services. Core classes will be shared with Android SDK. 

Please refer to [AppInsights-Home](https://github.com/Microsoft/AppInsights-Home) for general applicaiton insights documentation.

Getting started
---------------

  1. Recommended IDE is IntelliJ idea. When you first open either of the two projects, do it by ‘Import Project’ rather than ‘Open Project’ This will start from the Gradle files and recreate all the build and runtime dependencies in IntelliJ’s cache.
  2. You can use gradlew.bat on Windows to get gradle installed.

Using Eclipse IDE
---------------
1.	Install gradle from http://www.gradle.org/installation
2.	Add GRADLE_HOME/bin to your PATH environment variable
3.	In build.gradle add line [apply plugin: "eclipse"]  
4.	In Eclipse used File->Import Existing Project in a workspace.
5.	Use [gradle build] to build the project from the command line.

