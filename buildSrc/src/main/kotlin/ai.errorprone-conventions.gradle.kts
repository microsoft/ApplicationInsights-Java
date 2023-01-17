import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

dependencies {
  errorprone("com.google.errorprone:error_prone_core")
}

val disableErrorProne = properties["disableErrorProne"]?.toString()?.toBoolean() ?: false

tasks {
  withType<JavaCompile>().configureEach {
    with(options) {
      errorprone {
        if (disableErrorProne) {
          logger.warn("Errorprone has been disabled. Build may not result in a valid PR build.")
          isEnabled.set(false)
        }

        disableWarningsInGeneratedCode.set(true)
        allDisabledChecksAsWarnings.set(true)

        excludedPaths.set(".*/build/generated/.*")

        if (System.getenv("CI") == null) {
          disable("SystemOut")
        }

        // Still Java 8
        disable("Varifier")

        // Intellij does a nice job of displaying parameter names
        disable("BooleanParameter")

        // Needed for legacy 2.x bridge
        disable("JavaUtilDate")

        // Doesn't work well with Java 8
        disable("FutureReturnValueIgnored")

        // Require Guava
        disable("AutoValueImmutableFields")
        disable("StringSplitter")
        disable("ImmutableMemberCollection")

        // Don't currently use this (to indicate a local variable that's mutated) but could
        // consider for future.
        disable("Var")

        // Don't support Android without desugar
        disable("AndroidJdkLibsChecker")
        disable("Java7ApiChecker")
        disable("StaticOrDefaultInterfaceMethod")

        // needed temporarily while hosting azure-monitor-opentelemetry-exporter in this repo
        disable("MissingSummary")
        disable("UnnecessaryDefaultInEnumSwitch")
        disable("InconsistentOverloads")

        // consider enabling this after removing azure-monitor-exporter from this repo
        disable("CanIgnoreReturnValueSuggester")

        // YodaConditions may improve safety in some cases. The argument of increased
        // cognitive load is dubious.
        disable("YodaCondition")

        if (name.contains("Jmh")) {
          disable("MemberName")
        }
      }
    }
  }
}
