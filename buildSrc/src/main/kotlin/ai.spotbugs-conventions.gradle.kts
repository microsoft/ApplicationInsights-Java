plugins {
  id("com.github.spotbugs")
}

spotbugs {
  excludeFilter.set(
    file("${rootProject.rootDir}/gradle/spotbugs-exclude.xml")
  )
  omitVisitors.addAll(
    // we only use spotbugs for the findsecbugs plugin, and suppress anything else that gets flagged
    // since we use errorprone instead for this kind of static analysis
    //
    // the visitor names can be found in https://github.com/spotbugs/spotbugs/blob/master/spotbugs/etc/findbugs.xml
    // and https://github.com/find-sec-bugs/find-sec-bugs/blob/master/findsecbugs-plugin/src/main/resources/metadata/findbugs.xml
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
    "DumbMethodInvocations",
    "DontReusePublicIdentifiers"
  )
}

tasks {
  named("spotbugsTest") {
    enabled = false
  }
  
  withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("xml") {
      required.set(false)
    }
    // SpotBugs 6.2.x is stricter about missing classes and fails with exit code 3
    // We only use SpotBugs for the findsecbugs plugin (security analysis) and suppress everything else
    // So ignoring failures here is safe since we use errorprone for other static analysis
    ignoreFailures = true
  }
}

dependencies {
  // this is required for compliance
  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}
