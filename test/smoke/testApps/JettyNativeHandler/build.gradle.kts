plugins {
    id("ai.java-conventions")
    id "org.springframework.boot" version "2.1.7.RELEASE"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter:2.1.7.RELEASE")

    // needs to be same version used in fakeIngestion server
    implementation("org.eclipse.jetty:jetty-server:9.4.7.v20170914")
}
