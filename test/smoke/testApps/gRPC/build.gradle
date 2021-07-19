plugins {
    id("ai.java-conventions")
    id "org.springframework.boot" version "2.1.7.RELEASE"
    id "com.google.protobuf" version "0.8.14"
}

ext.testAppArtifactDir = jar.destinationDirectory
ext.testAppArtifactFilename = jar.archiveFileName.get()

def protobufVersion = "3.6.1"
def grpcVersion = "1.16.1"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all()*.plugins {
            grpc {}
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
