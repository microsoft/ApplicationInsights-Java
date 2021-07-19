plugins {
    id("ai.java-conventions")
    id "org.springframework.boot" version "2.2.0.RELEASE"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.2.0.RELEASE")
    implementation("org.springframework.cloud:spring-cloud-stream:2.2.1.RELEASE")
    implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka:2.2.1.RELEASE")
    implementation("org.springframework.kafka:spring-kafka:2.3.1.RELEASE")
    implementation("org.springframework:spring-tx:5.2.0.RELEASE")
}
