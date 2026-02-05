plugins {
  `maven-publish`
  signing
}

publishing {
  publications {
    register<MavenPublication>("maven") {
      plugins.withId("java-platform") {
        from(components["javaPlatform"])
      }
      plugins.withId("java-library") {
        from(components["java"])
      }

      versionMapping {
        allVariants {
          fromResolutionResult()
        }
      }

      afterEvaluate {
        groupId = "com.microsoft.azure"
        artifactId = base.archivesName.get()

        pom.description.set(
          project.description
            ?: "Microsoft Application Insights Java Agent."
        )
      }

      pom {
        name.set("Microsoft Application Insights Java Agent")
        url.set("https://github.com/Microsoft/ApplicationInsights-Java")

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
