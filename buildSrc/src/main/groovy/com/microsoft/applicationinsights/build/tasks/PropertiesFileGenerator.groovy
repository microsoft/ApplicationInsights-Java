package com.microsoft.applicationinsights.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PropsFileGen extends DefaultTask {
    File targetFile
    String comment = "--- No Comment ---"
    private Properties props = new Properties()

    def property(String key, String value) { props.setProperty(key, value) }

    @TaskAction
    def generate() {
        def targetDir = targetFile.getParentFile()
        targetDir.mkdirs()
        FileOutputStream out = new FileOutputStream(targetFile.getAbsolutePath())
        props.store(out, comment)
        out.close()
    }
}
