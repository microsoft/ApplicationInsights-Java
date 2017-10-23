package com.microsoft.applicationinsights.agent.internal.agent;


import org.objectweb.asm.ClassWriter;


/**
 * Created by Dhaval Doshi(dhdoshi)
 *
 * This class overwrites default class writer of ASM to use the ClassLoader
 * provided by DefaultByteCode transformer (This loader essentially has all
 * the required classes already loaded)
 */
class CustomClassWriter extends ClassWriter {


    ClassLoader classLoader;

    public CustomClassWriter(int writerFlag, ClassLoader loader)
    {
        super(writerFlag);
        this.classLoader = loader;
    }

    /**
     * This method returns common super class for both the classes. If no super class
     * is present it returns java/lang/Object class.
     * @param className1
     * @param className2
     * @return The String for the common super class of both the classes
     */
    protected String getCommonSuperClass(String className1, String className2)
    {
        Class class1;
        Class class2;
        try
        {
            class1 = Class.forName(className1.replace('/', '.'), false, this.classLoader);
            class2 = Class.forName(className2.replace('/', '.'), false, this.classLoader);
        }
        catch (Exception th) {
            throw new RuntimeException(th.getMessage());
        }

        if (class1.isAssignableFrom(class2)) {
            return className1;
        }
        if (class2.isAssignableFrom(class1)) {
            return className2;
        }

        if ((class1.isInterface()) || (class2.isInterface())) {
            return "java/lang/Object";
        }

        do {
            class1 = class1.getSuperclass();
        }
        while (!(class1.isAssignableFrom(class2)));
        return class1.getName().replace('.', '/');
    }

}
