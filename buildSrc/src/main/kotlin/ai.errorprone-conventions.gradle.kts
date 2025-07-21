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

        // Ignore warnings for generated classes
        excludedPaths.set(".*/build/generated/.*")

        // it's very convenient to debug stuff in the javaagent using System.out.println
        // and we don't want to conditionally only check this in CI
        // because then the remote gradle cache won't work for local builds
        // so we check this via checkstyle instead
        disable("SystemOut")

        disable("BooleanParameter")

        // Doesn't work well with Java 8
        disable("FutureReturnValueIgnored")

        // Needs Java 9+
        disable("JavaDurationGetSecondsToToSeconds")

        // Still Java 8
        disable("Varifier")

        // Doesn't currently use Var annotations.
        disable("Var") // "-Xep:Var:OFF"

        // ImmutableRefactoring suggests using com.google.errorprone.annotations.Immutable,
        // but currently uses javax.annotation.concurrent.Immutable
        disable("ImmutableRefactoring")

        // AutoValueImmutableFields suggests returning Guava types from API methods
        disable("AutoValueImmutableFields")
        // Suggests using Guava types for fields but we don't use Guava
        disable("ImmutableMemberCollection")

        // TODO (trask) use animal sniffer
        disable("AndroidJdkLibsChecker")

        // apparently disabling android doesn't disable this
        disable("StaticOrDefaultInterfaceMethod")

        // TODO (trask) Fix the underlying smoke test methods
        disable("InconsistentOverloads")

        // We don't depend on Guava so use normal splitting
        disable("StringSplitter")

        // allow UPPERCASE type parameter names
        disable("TypeParameterNaming")

        // We end up using obsolete types if a library we're instrumenting uses them.
        disable("JavaUtilDate")

        disable("CanIgnoreReturnValueSuggester")

        // YodaConditions may improve safety in some cases. The argument of increased
        // cognitive load is dubious.
        disable("YodaCondition")

        disable("NonFinalStaticField")

        // Requires adding compile dependency to JSpecify
        disable("AddNullMarkedToPackageInfo")
      }
    }
  }
}
