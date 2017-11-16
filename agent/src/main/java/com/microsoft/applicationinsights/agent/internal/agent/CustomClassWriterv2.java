package com.microsoft.applicationinsights.agent.internal.agent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class overwrites default class writer of ASM to use the ClassLoader
 * provided by DefaultByteCode transformer (This loader essentially has all
 * the required classes already loaded)
 */
public class CustomClassWriterv2 extends org.objectweb.asm.ClassWriter {

    private ClassLoader classLoader;

    public CustomClassWriterv2(int writerFlag, ClassLoader loader) {
        super(writerFlag);
        this.classLoader = loader;
    }

    /**
     * This method returns common super class for both the classes without actually loading
     * the class using forName(). If no super class is present it returns java/lang/Object class.
     * @param type1
     * @param type2
     * @return The String for the common super class of both the classes
     */
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        try {
            ClassReader info1 = typeInfo(type1);
            ClassReader info2 = typeInfo(type2);
            if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (typeImplements(type2, info2, type1)) {
                    return type1;
                }
                if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    if (typeImplements(type1, info1, type2)) {
                        return type2;
                    }
                }
                return "java/lang/Object";
            }
            if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (typeImplements(type1, info1, type2)) {
                    return type2;
                } else {
                    return "java/lang/Object";
                }
            }
            StringBuilder b1 = typeAncestors(type1, info1);
            StringBuilder b2 = typeAncestors(type2, info2);
            String result = "java/lang/Object";
            int end1 = b1.length();
            int end2 = b2.length();
            while (true) {
                int start1 = b1.lastIndexOf(";", end1 - 1);
                int start2 = b2.lastIndexOf(";", end2 - 1);
                if (start1 != -1 && start2 != -1
                        && end1 - start1 == end2 - start2) {
                    String p1 = b1.substring(start1 + 1, end1);
                    String p2 = b2.substring(start2 + 1, end2);
                    if (p1.equals(p2)) {
                        result = p1;
                        end1 = start1;
                        end2 = start2;
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Determines parent class
     * @param type
     * @param info
     * @return
     * @throws IOException
     */
    private StringBuilder typeAncestors(String type, ClassReader info) throws IOException {
        StringBuilder b = new StringBuilder();
        while (!"java/lang/Object".equals(type)) {
            b.append(';').append(type);
            type = info.getSuperName();
            info = typeInfo(type);
        }
        return b;
    }


    /**
     * Determines common interface implementation
     * @param type
     * @param info
     * @param itf
     * @return
     * @throws IOException
     */
    private boolean typeImplements(String type, ClassReader info, String itf) throws IOException {
        while (!"java/lang/Object".equals(type)) {
            String[] itfs = info.getInterfaces();
            for (String itf2 : itfs) {
                if (itf2.equals(itf)) {
                    return true;
                }
            }
            for (String itf1 : itfs) {
                if (typeImplements(itf1, typeInfo(itf1), itf)) {
                    return true;
                }
            }
            type = info.getSuperName();
            info = typeInfo(type);
        }
        return false;
    }

    /**
     * Generates ASM Classwriter from the Input Stream for detailed information
     * @param type
     * @return
     * @throws IOException
     */
    private ClassReader typeInfo(final String type) throws IOException {
        InputStream is = classLoader.getResourceAsStream(type + ".class");
        return new ClassReader(is);
    }
}


