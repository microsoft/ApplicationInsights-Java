plugins {
    id("ai.java-conventions")
    id "org.springframework.boot" version "2.1.7.RELEASE"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")

    implementation("com.squareup.okhttp3:okhttp:3.12.1")
}
