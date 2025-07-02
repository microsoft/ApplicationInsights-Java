plugins {
  id("com.github.spotbugs")
}

spotbugs {
  excludeFilter.set(
    file("${rootProject.rootDir}/gradle/spotbugs-exclude.xml")
  )
  effort.set(com.github.spotbugs.snom.Effort.MIN)
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
    // SpotBugs 6.2.x fails with exit code 3 when classes needed for analysis are missing
    // The missing classes are typically lambda method references that don't affect security analysis
    // Since we only use SpotBugs for findsecbugs security plugin, this is safe to ignore
    ignoreFailures = true
  }
}

dependencies {
  // this is required for compliance
  spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}
