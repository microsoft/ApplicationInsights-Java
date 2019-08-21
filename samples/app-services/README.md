App Services Example with Application Insights

* Update `resourceGroup`, `appName` and `region` in [pom.xml](pom.xml)

* Optionally update `os`, `javaVersion` and `webContainer` in [pom.xml](pom.xml)

* Update instrumentation key in [ApplicationInsights.xml](src/main/resources/ApplicationInsights.xml)

* az login

* az account set --subscription <subscription Id>

* Deploy using `mvn clean package azure-webapp:deploy`
