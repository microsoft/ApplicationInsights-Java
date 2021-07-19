import java.time.Duration

plugins {
  id("idea")

  id("com.github.ben-manes.versions")
  id("com.diffplug.spotless")
  id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
  version = "1.3.1+ai.patch.1"
}

nexusPublishing {
  packageGroup.set("io.opentelemetry")

  repositories {
    sonatype {
      username.set(System.getenv("SONATYPE_USER"))
      password.set(System.getenv("SONATYPE_KEY"))
    }
  }

  connectTimeout.set(Duration.ofMinutes(5))
  clientTimeout.set(Duration.ofMinutes(5))

  transitionCheckOptions {
    // We have many artifacts so Maven Central takes a long time on its compliance checks. This sets
    // the timeout for waiting for the repository to close to a comfortable 50 minutes.
    maxRetries.set(300)
    delayBetween.set(Duration.ofSeconds(10))
  }
}

// Enable after verifying Maven Central publishing once through manual closing
// tasks.release.finalizedBy tasks.closeAndReleaseRepository

description = "OpenTelemetry instrumentations for Java"

spotless {
  // this formatting is applied at the root level, as some of these files are not in a submodules
  // and would be missed otherwise
  format("misc") {
    target(".gitignore", "*.md", "docs/**/*.md")
    indentWithSpaces()
    trimTrailingWhitespace()
    endWithNewline()
  }
}
