plugins {
    id("ai.java-conventions")
    id("war")
}

war {
    // this is done to remove the version from the archive file name
    // to make span name verification simpler
    archiveFileName = project.name + ".war"
}

ext.testAppArtifactDir = war.destinationDirectory
ext.testAppArtifactFilename = project.name + ".war"

ext.dependencyContainers = ["mysql:5", "postgres:11", "mcr.microsoft.com/mssql/server:2017-latest"]

dependencies {
    implementation("org.hsqldb:hsqldb:2.3.6" // 2.4.0+ requires Java 8+)
            implementation("mysql:mysql-connector-java:5.1.49")
            implementation("org.postgresql:postgresql:42.2.16.jre7")
            implementation("com.microsoft.sqlserver:mssql-jdbc:8.4.1.jre8")

            compileOnly("javax.servlet:javax.servlet-api:3.0.1")
}
