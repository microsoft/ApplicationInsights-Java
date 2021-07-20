import net.ltgt.gradle.errorprone.errorprone

plugins {
  id("net.ltgt.errorprone")
}

dependencies {
  errorprone("com.google.errorprone:error_prone_core")
}

val disableErrorProne = gradle.startParameter.projectProperties.get("disableErrorProne")?.toBoolean()
  ?: false

tasks {
  withType<JavaCompile>().configureEach {
    options.errorprone {
      isEnabled.set(!disableErrorProne)

      disableWarningsInGeneratedCode.set(true)
      allDisabledChecksAsWarnings.set(true)

      excludedPaths.set(".*/build/generated/.*")

      if (System.getenv("CI") == null) {
        disable("SystemOut")
      }

      // TEMPORARILY until time to revisit
      disable("WildcardImport")
      disable("BadImport")
      disable("BooleanParameter")
      disable("JavaUtilDate")
      disable("HashCodeToString")
      disable("ImmutableEnumChecker")
      disable("JavaTimeDefaultTimeZone")

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

      // Common to avoid an allocation. Revisit if it's worth opt-in suppressing instead of
      // disabling entirely.
      // TODO (trask) consider enabling
      disable("MixedMutabilityReturnType")

      // Limits API possibilities
      disable("NoFunctionalReturnType")

      // We don't use tools that recognize.
      disable("InlineMeSuggester")
      disable("DoNotCallSuggester")

      if (name.contains("Jmh") || name.contains("Test")) {
        disable("HashCodeToString")
        disable("MemberName")
      }
    }
  }
}
