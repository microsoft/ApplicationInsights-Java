plugins {
  id("ai.java-conventions")
}

dependencies {
  implementation("com.google.guava:guava")
  implementation("junit:junit:4.13.2")
  implementation("org.apache.commons:commons-lang3:3.7")
  implementation(project(":test:smoke:framework:utils"))
  implementation(project(":test:fakeIngestion:standalone"))
}
