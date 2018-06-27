package com.microsoft.applicationinsights.agent.internal.agent;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

public class CustomClassWriterTest {

  @Test
  public void testCommonSuperClass() {

    String extendedClass1BinaryName = extendedClass1.class.getName().replace('.', '/');
    String extendedClass2BinaryName = extendedClass2.class.getName().replace('.', '/');
    String baseClassBinaryName = baseClass.class.getName().replace('.', '/');
    CustomClassWriter classWriter =
        new CustomClassWriter(ClassWriter.COMPUTE_FRAMES, getClass().getClassLoader());
    Assert.assertEquals(
        baseClassBinaryName,
        classWriter.getCommonSuperClass(extendedClass1BinaryName, extendedClass2BinaryName));

    String classABinaryName = classA.class.getName().replace('.', '/');
    String interfaceABinaryName = interfaceA.class.getName().replace('.', '/');
    Assert.assertEquals(
        interfaceABinaryName,
        classWriter.getCommonSuperClass(classABinaryName, interfaceABinaryName));

    String objectClassBinaryName = Object.class.getName().replace('.', '/');
    Assert.assertEquals(
        objectClassBinaryName,
        classWriter.getCommonSuperClass(classABinaryName, objectClassBinaryName));
  }

  private interface interfaceA {
    void test();
  }

  private class classA implements interfaceA {

    @Override
    public void test() {}
  }

  private class baseClass {}

  private class extendedClass1 extends baseClass {}

  private class extendedClass2 extends baseClass {}
}
