val snapshot = false

allprojects {
  var ver = "1.14.0.1"
  if (findProperty("otel.stable") != "true") {
    ver += "-alpha"
  }
  if (snapshot) {
    ver += "-SNAPSHOT"
  }
  version = ver
}
