import com.google.protobuf.gradle.*

plugins {
  id("ai.smoke-test-jar")
  id("com.google.protobuf") version "0.9.5"
}

val grpcVersion = "1.26.0" // first version with support for arm64
val nettyVersion = "4.1.42.Final"

protobuf {
  protoc {
    // Download compiler rather than using locally installed version:
    artifact = "com.google.protobuf:protoc:3.11.0" // needs to be aligned with the grpc version above
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
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
  implementation("org.springframework.boot:spring-boot-starter-web:2.5.12")

  implementation("io.grpc:grpc-core:$grpcVersion")
  implementation("io.grpc:grpc-netty:$grpcVersion")
  implementation("io.grpc:grpc-protobuf:$grpcVersion")
  implementation("io.grpc:grpc-stub:$grpcVersion")
  // need to use netty version aligned with grpc version, and not managed version used in agent
  implementation(enforcedPlatform("io.netty:netty-bom:$nettyVersion"))
}

afterEvaluate {
  // Classpath when compiling protos, we add dependency management directly
  // since it doesn't follow Gradle conventions of naming / properties.
  dependencies {
    add("compileProtoPath", platform(project(":dependencyManagement")))
    add("smokeTestCompileProtoPath", platform(project(":dependencyManagement")))
  }
}
