package com.microsoft.applicationinsights.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

abstract class AiSmokeTestExtension {
  abstract val testAppArtifactDir: DirectoryProperty

  abstract val testAppArtifactFilename: Property<String>

  abstract val dependencyContainers: ListProperty<String>
}
