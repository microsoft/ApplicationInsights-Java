import com.microsoft.applicationinsights.gradle.AiSmokeTestExtension

plugins {
  id("ai.java-conventions")
  id("ai.spotless-conventions")
}

// TODO (trask) this is copy-paste from :test:smoke
val sharedOutputRoot = project(":test:smoke").projectDir.absolutePath + "/shared-tmp"
val sharedResourcesDir = "$sharedOutputRoot/resources"

val aiSmokeTest = extensions.create<AiSmokeTestExtension>("aiSmokeTest")

sourceSets {
  create("smokeTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

val smokeTestImplementation by configurations.getting {
  extendsFrom(configurations.implementation.get())
}

configurations["smokeTestRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

dependencies {
  smokeTestImplementation(project(":test:smoke:framework:testCore"))
  smokeTestImplementation(project(":test:smoke:framework:utils"))
  smokeTestImplementation(project(":test:fakeIngestion:standalone"))

  // NOTE not updating smoke tests to JUnit 5, because AiSmokeTest has deep dependency on JUnit 4 infra,
  // and so would take a good amount of work, and eventually want to migrate to otel smoke tests anyways
  smokeTestImplementation("org.hamcrest:hamcrest-core:1.3")
  smokeTestImplementation("org.hamcrest:hamcrest-library:1.3")
  smokeTestImplementation("junit:junit:4.13.2")
}

tasks {
  // This task addresses the issue of dependency containers not existing when the app runs and the test fails due to timeout.
  val pullDependencyContainers by registering {
    doLast {
      aiSmokeTest.dependencyContainers.get().forEach { dc ->
        logger.info("Pulling $dc...")
        exec {
          executable = "docker"
          args = listOf("pull", dc)
        }
      }
    }
  }

  val smokeTest by registering(Test::class) {
    // this is just to force building the agent first
    dependsOn(":agent:agent:shadowJar")

    // TODO this adds the whole tree rooted at :appServers. Could this depend on :appServers which depends on :appServers:*:build?
    dependsOn(project(":test:smoke:appServers").getTasksByName("buildDockerImage", true))
    dependsOn(assemble)
    dependsOn(pullDependencyContainers)

    // TODO (trask) is this still a problem?
    //outputs.upToDateWhen { false }
  }

  // copies test app WARs and shared resources into smoke test resources folder

  named<Copy>("processSmokeTestResources") {
    from(aiSmokeTest.testAppArtifactDir.file(aiSmokeTest.testAppArtifactFilename.get()))
    from(sharedResourcesDir)
  }

  // FIXME (trask) set this via system property command line
//  val generateProperties by registering(WriteProperties::class) {
//    outputFile = File(processSmokeTestResources.destinationDir, "testInfo.properties")
//    property("ai.smoketest.testAppWarFile", aiSmokeTest.testAppArtifactFilename)
//  }
}
