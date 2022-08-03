plugins {
  id("com.github.spotbugs")
}

spotbugs {
  omitVisitors.addAll(
    // we only use spotbugs for the findsecbugs plugin, and suppress anything else that gets flagged
    // since we use errorprone instead for this kind of static analysis
    "CheckRelaxingNullnessAnnotation",
    "FindDeadLocalStores",
    "FindNullDeref",
    "FindReturnRef",
    "FindUselessObjects",
    "MethodReturnCheck",
    "MutableStaticFields",
    "Naming",
    "RuntimeExceptionCapture",
    "UnreadFields",
    "DumbMethodInvocations"
  )
}

tasks {
  named("spotbugsTest") {
    enabled = false
  }
}

dependencies {
  // this is required for compliance
  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}
