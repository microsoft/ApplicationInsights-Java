package com.microsoft.ajl.simple;

import com.google.common.base.Joiner;
import com.microsoft.applicationinsights.TelemetryClient;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/verifyJava7")
    public String verifyJava7() throws IOException {

        URL url = TelemetryClient.class.getProtectionDomain().getCodeSource().getLocation();

        InputStream in = url.openStream();
        JarInputStream jarIn = new JarInputStream(in);

        List<String> java8Classnames = new ArrayList<>();

        JarEntry jarEntry;
        while ((jarEntry = jarIn.getNextJarEntry()) != null) {
            String name = jarEntry.getName();
            if (name.endsWith(".class")) {
                VersionCapturingClassVisitor cv = new VersionCapturingClassVisitor();
                new ClassReader(jarIn).accept(cv, 0);
                if (name.equals("com/microsoft/applicationinsights/core/dependencies/xmlpull") && cv.version == 196653) {
                    // strange..
                    continue;
                }
                if (cv.version > 51) {
                    java8Classnames.add(name.replace('/', '.'));
                }
            }
        }

        jarIn.close();
        in.close();

        if (!java8Classnames.isEmpty()) {
            throw new AssertionError("Found Java 8+ classes: " + Joiner.on(", ").join(java8Classnames));
        }
        return "OK";
    }

    static class VersionCapturingClassVisitor extends ClassVisitor {

        int version;

        private VersionCapturingClassVisitor() {
            super(Opcodes.ASM7);
        }

        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.version = version;
        }
    }
}
