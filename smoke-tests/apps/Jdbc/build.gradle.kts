plugins {
  id("ai.smoke-test-war")
}

dependencies {
  implementation("org.hsqldb:hsqldb:2.3.6") // 2.4.0+ requires Java 8+
  implementation("mysql:mysql-connector-java:5.1.49")
  implementation("org.postgresql:postgresql:42.2.16.jre7")
  implementation("com.microsoft.sqlserver:mssql-jdbc:8.4.1.jre8")
}
