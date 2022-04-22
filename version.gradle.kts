val snapshot = false

allprojects {
  var ver = "1.13.0+ai.patches"
  if (findProperty("otel.stable") != "true") {
    ver += "-alpha"
  }
  if (snapshot) {
    ver += "-SNAPSHOT"
  }
  version = ver
}
