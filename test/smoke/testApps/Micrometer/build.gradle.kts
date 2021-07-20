plugins {
    id("ai.java-conventions")
    id "org.springframework.boot" version "2.1.7.RELEASE"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")
    implementation("io.micrometer:micrometer-core:1.4.1")
}
