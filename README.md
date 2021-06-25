[![Build Status](https://github-private.visualstudio.com/microsoft/_apis/build/status/CDPX/applicationinsights-java/applicationinsights-java-Windows-Buddy-master?branchName=refs%2Fpull%2F1583%2Fmerge)](https://github-private.visualstudio.com/microsoft/_build/latest?definitionId=224&branchName=refs%2Fpull%2F1583%2Fmerge)

# Application Insights for Java

See documentation at https://docs.microsoft.com/en-us/azure/azure-monitor/app/java-in-process-agent.

## If you need to build locally

Download the main repo and submodule:

```
git clone https://github.com/microsoft/ApplicationInsights-Java
cd ApplicationInsights-Java
git submodule init
git submodule update
```

Publish all the artifacts from the submodule to your local maven repository:

```
cd otel
./gradlew publishToMavenLocal
cd ..
```

Build the agent jar file:

```
./gradlew -DskipWinNative=true :agent:agent:shadowJar
```

The agent jar file should now be available under `agent/agent/build/libs`.

## If you are contributing...

We follow the same
[style guidelines](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/style-guideline.md)
and
[recommended Intellij setup](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/intellij-setup.md)
as the OpenTelemetry Java Instrumentation repo.

While developing, if you find errorprone is getting in your way (e.g. it won't let you add
`System.out.println` to your code), you can disable it by adding the following to your
`~/.gradle/gradle.properties`:

```
disableErrorProne=true
```

## Microsoft Open Source Code of Conduct

This project has adopted the
[Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the
[Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/)
or contact [opencode@microsoft.com](mailto:opencode@microsoft.com)
with any additional questions or comments.
