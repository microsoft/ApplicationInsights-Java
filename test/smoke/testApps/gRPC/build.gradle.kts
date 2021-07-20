import com.google.protobuf.gradle.*

plugins {
  id("ai.smoke-test-jar")
  id("com.google.protobuf") version "0.8.16"
}

val grpcVersion = "1.16.1"

protobuf {
  protoc {
    // Download compiler rather than using locally installed version:
    artifact = "com.google.protobuf:protoc:3.3.0"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:${grpcVersion}"
    }
  }
  generateProtoTasks {
    all().configureEach {
      plugins {
        id("grpc")
      }
    }
  }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web:2.1.7.RELEASE")

  implementation("io.grpc:grpc-core:$grpcVersion")
  implementation("io.grpc:grpc-netty:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
}
