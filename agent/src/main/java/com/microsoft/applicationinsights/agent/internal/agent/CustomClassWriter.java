package com.microsoft.applicationinsights.agent.internal.agent;


import org.objectweb.asm.ClassWriter;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;


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
        System.out.println("Trace: Registering class loader from Code injector");
        System.out.println("Trace: Loaded class loaded :" + classLoader.toString());
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

        Class class1 = null;
        Class class2 = null;
        Object t1 = null;
        Object t2 = null;
        Method m;
        try {
            m = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[]{String.class});
            m.setAccessible(true);
            t1 = m.invoke(classLoader, className1.replace('/', '.'));
            t2 = m.invoke(classLoader, className2.replace('/', '.'));
        }
        catch (Exception e) {
            e.printStackTrace();
        }



//        if (className1.contains("AbstractHttpClient") || className2.contains("AbstractHttpClient")) {
//            return "java/lang/Object";
//        }
        //System.out.println("Trace: Default Class loader :" + getClass().getClassLoader().toString());
//        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
//        System.out.println("Trace: Context class loader is " + contextClassLoader.toString());

        System.out.println("TRACE: input className1 is " + className1);
        System.out.println("TRACE: input className2 is " + className2);

        try
        {
            if (t1 == null) {
                System.out.println("using for name");
                class1 = Class.forName(className1.replace('/', '.'), false, this.classLoader);
            }
            else {
                System.out.println("using classloader loaded class");
                class1 = classLoader.loadClass(className1.replace('/', '.'));
            }
            if (t2 == null) {
                System.out.println("using for name");
                class2 = Class.forName(className2.replace('/', '.'), false, this.classLoader);
            }
            else {
                System.out.println("using for name");
                class2 = classLoader.loadClass(className1.replace('/', '.'));
            }


            System.out.println("Trace: reflection generated Class 1 " + class1.toString());
            System.out.println("Trace: reflection generated Class 2 " + class2.toString());
            //Thread.dumpStack();
        }
        catch (Exception th) {
            throw new RuntimeException(th.getMessage());
        }

        if (class1.isAssignableFrom(class2)) {
            System.out.println("TRACE: class 1 is assigned from " + className2);
            return className1;
        }
        else if (class2.isAssignableFrom(class1)) {
            System.out.println("TRACE: class 1 is assigned from " + className2);
            return className2;
        }

        else if ((class1.isInterface()) || (class2.isInterface())) {
            System.out.println("TRACE: Returning java.lang.object");
            return "java/lang/Object";
        }

        else {
            do {
                class1 = class1.getSuperclass();
            }
            while (!(class1.isAssignableFrom(class2)));
            System.out.println("Trace: after loop " + class1.getName().replace('.', '/'));
            return class1.getName().replace('.', '/');
        }

    }


}
