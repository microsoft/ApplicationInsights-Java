plugins {
  id("ai.smoke-test-jar")
}

// Override default main class
ext.set("mainClassName", "com.microsoft.applicationinsights.smoketestapp.App")
