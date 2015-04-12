package com.microsoft.applicationinsights.build.tasks

import org.gradle.api.Task;
import org.gradle.api.DefaultTask
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class PropsFileGen extends DefaultTask {
    @Input
    String comment = "--- No Comment ---"

    @OutputFile
    File targetFile

    private final Properties props = new Properties()

    PropsFileGen() {
        getOutputs().upToDateWhen(new Spec<Task>() {
            public boolean isSatisfiedBy(Task element) {
                return propsAreEqual();
            }
        });
    }

    def property(String key, String value) {
        props.setProperty(key, value)
    }

    @TaskAction
    private def generate() {
        def targetDir = targetFile.getParentFile()
        targetDir.mkdirs()
        FileOutputStream out = new FileOutputStream(targetFile.getAbsolutePath())
        props.store(out, comment)
        out.close()
    }

    private boolean propsAreEqual() {
        if (!targetFile.exists()) {
            return false;
        }

        Properties oldProps = new Properties();
        FileInputStream inFileStream = new FileInputStream(targetFile);
        oldProps.load(inFileStream);

        return oldProps.equals(props);
    }
}
