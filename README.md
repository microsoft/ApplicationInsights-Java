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

## Microsoft Open Source Code of Conduct

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/). For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.
