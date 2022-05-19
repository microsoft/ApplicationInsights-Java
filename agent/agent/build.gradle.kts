import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("ai.java-conventions")
  id("ai.shadow-conventions")
  id("maven-publish")
}

base.archivesName.set("applicationinsights-agent")

java {
  withJavadocJar()
  withSourcesJar()
}

// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration collects libs that will be placed in the agent classloader, isolated from the instrumented application code
val javaagentLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration stores the upstream agent dep that's extended by this project
val upstreamAgent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

val otelInstrumentationVersion: String by project
val otelInstrumentationAlphaVersion: String by project

dependencies {

  // required to access OpenTelemetryAgent
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$otelInstrumentationAlphaVersion")

  bootstrapLibs(project(":agent:agent-bootstrap"))

  javaagentLibs(project(":agent:agent-tooling"))

  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent:$otelInstrumentationVersion")
}

val javaagentDependencies = dependencies

// collect all instrumentation sub projects
project(":agent:instrumentation").subprojects {
  val subProj = this
  plugins.withId("ai.javaagent-instrumentation") {
    javaagentDependencies.run {
      add(javaagentLibs.name, project(subProj.path))
    }
  }
}

tasks {
  jar {
    enabled = false
  }

  // building the final javaagent jar is done in 3 steps:

  // 1. all distro-specific javaagent libs are relocated (by the ai.shadow-conventions plugin)
  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

    archiveFileName.set("javaagentLibs-relocated.jar")

    dependencies {
      // exclude known bootstrap dependencies - they can't appear in the inst/ directory
      exclude(dependency("org.slf4j:slf4j-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-api"))
      exclude(dependency("io.opentelemetry:opentelemetry-api-metrics"))
      exclude(dependency("io.opentelemetry:opentelemetry-context"))
      exclude(dependency("io.opentelemetry:opentelemetry-semconv"))

      // TODO (trask) Azure SDK: why is this included in azure-core?
      exclude(dependency("io.netty:netty-tcnative-boringssl-static"))
    }
  }

  // 2. the distro javaagent libs are then isolated - moved to the inst/ directory
  // having a separate task for isolating javaagent libs is required to avoid duplicates with the upstream javaagent
  // duplicatesStrategy in shadowJar won't be applied when adding files with with(CopySpec) because each CopySpec has
  // its own duplicatesStrategy
  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    isolateClasses(relocateJavaagentLibs.get().outputs.files)

    into("$buildDir/isolated/javaagentLibs")
  }

  // 3. the relocated and isolated javaagent libs are merged together with the bootstrap libs (which undergo relocation
  // in this task) and the upstream javaagent jar; duplicates are removed
  shadowJar {
    configurations = listOf(bootstrapLibs, upstreamAgent)

    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)

    archiveClassifier.set("all")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
      attributes(
        mapOf(
          "Premain-Class" to "com.microsoft.applicationinsights.agent.Agent",
          // Agent-Class is provided only for dynamic attach in the first line of main
          // there are many problematic edge cases around dynamic attach any later than that
          "Agent-Class" to "com.microsoft.applicationinsights.agent.Agent",
          "Can-Redefine-Classes" to true,
          "Can-Retransform-Classes" to true
        )
      )
    }
  }

  // a separate task to create a no-classifier jar that's exactly the same as the -all one
  // because a no-classifier (main) jar is required by sonatype
  val mainShadowJar by registering(Jar::class) {
    archiveClassifier.set("")

    from(zipTree(shadowJar.get().archiveFile))

    manifest {
      attributes(shadowJar.get().manifest.attributes)
    }
  }

  assemble {
    dependsOn(shadowJar, mainShadowJar)
  }

  val t = this
  publishing {
    publications {
      register<MavenPublication>("maven") {
        artifactId = "applicationinsights-agent"
        groupId = "com.microsoft.azure"
        version = project.version.toString()

        artifact(shadowJar)
        artifact(mainShadowJar)
        artifact(t.named("sourcesJar"))
        artifact(t.named("javadocJar"))

        pom {
          name.set("Microsoft Application Insights Java Agent")
          description.set("Microsoft Application Insights Java Agent")
          url.set("https://github.com/Microsoft/ApplicationInsights-Java")
          packaging = "jar"

          licenses {
            license {
              name.set("MIT License")
              url.set("http://www.opensource.org/licenses/mit-license.php")
            }
          }

          developers {
            developer {
              id.set("Microsoft")
              name.set("Microsoft")
            }
          }

          scm {
            connection.set("scm:git:git://github.com/Microsoft/ApplicationInsights-Java.git")
            url.set("scm:git:https://github.com/Microsoft/ApplicationInsights-Java")
          }
        }
      }
    }
  }
}

fun CopySpec.isolateClasses(jars: Iterable<File>) {
  jars.forEach {
    from(zipTree(it)) {
      into("inst")
      rename("^(.*)\\.class\$", "\$1.classdata")
      // Rename LICENSE file since it clashes with license dir on non-case sensitive FSs (i.e. Mac)
      rename("""^LICENSE$""", "LICENSE.renamed")
    }
  }
  from("${rootProject.projectDir}/NOTICE")
  from("${rootProject.projectDir}/LICENSE")
}
