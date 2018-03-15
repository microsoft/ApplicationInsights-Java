/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.internal.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.microsoft.applicationinsights.internal.logger.InternalLogger;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Utililty methods for dealing with reflection
 *
 * Created by gupele on 8/7/2016.
 */
public final class ReflectionUtils {
    /**
     * Creates an instance from its name. We suppress Java compiler warnings for Generic casting
     *
     * Note that currently we 'swallow' all exceptions and simply return null if we fail
     *
     * @param className The class we create an instance of
     * @param interfaceClass The class' parent interface we wish to work with
     * @param <T> The class type to create
     * @return The instance or null if failed
     */
    @SuppressWarnings("unchecked")
    public static <T> T createInstance(String className, Class<T> interfaceClass) {
        try {
            if (LocalStringsUtils.isNullOrEmpty(className)) {
                InternalLogger.INSTANCE.error("Failed to create empty class name");
                return null;
            }

            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            T instance = (T)clazz.newInstance();

            return instance;
        } catch (ClassCastException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (ClassNotFoundException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (InstantiationException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (IllegalAccessException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        }

        return null;
    }

    /**
     * Creates an instance from its name. We suppress Java compiler warnings for Generic casting
     * The class is created by using a constructor that has one parameter which is sent to the method
     *
     * Note that currently we 'swallow' all exceptions and simply return null if we fail
     *
     * @param className The class we create an instance of
     * @param interfaceClass The class' parent interface we wish to work with
     * @param argumentClass Type of class to use as argument for Ctor
     * @param argument The argument to pass the Ctor
     * @param <T> The class type to create
     * @param <A> The class type as the Ctor argument
     * @return The instance or null if failed
     */
    @SuppressWarnings("unchecked")
    public static  <T, A> T createInstance(String className, Class<T> interfaceClass, Class<A> argumentClass, A argument) {
        try {
            if (LocalStringsUtils.isNullOrEmpty(className)) {
                InternalLogger.INSTANCE.error("Failed to create empty class name");
                return null;
            }

            Class<?> clazz = Class.forName(className).asSubclass(interfaceClass);
            Constructor<?> clazzConstructor = clazz.getConstructor(argumentClass);
            T instance = (T)clazzConstructor.newInstance(argument);
            return instance;
        } catch (ClassCastException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (ClassNotFoundException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (InstantiationException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (IllegalAccessException e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            InternalLogger.INSTANCE.error("Failed to create %s, Exception : %s", className, ExceptionUtils.getStackTrace(e));
        }

        return null;
    }

    /**
     *
     * @param object - The instance we work with
     * @param methodName - The method to activate
     * @param value - The value to pass to the method
     * @param argumentClass - The argument class
     * @param <V> - Generic for value class
     * @param <A> - Generic for argument class
     * @return - True if method found and activation was ok, otherwise false.
     */
    public static <V, A> boolean activateMethod(Object object, String methodName, V value, A argumentClass) {
        Class<?> clazz = object.getClass();
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, String.class);
            method.invoke(object, value);
            return true;
        } catch (NoSuchMethodException e) {
            InternalLogger.INSTANCE.error("Failed to call method %s .NoSuchMethodException",  methodName);
        } catch (InvocationTargetException e) {
            InternalLogger.INSTANCE.error("Failed to call method %s .InvocationTargetException",  methodName);
        } catch (IllegalAccessException e) {
            InternalLogger.INSTANCE.error("Failed to call method %s .IllegalAccessException",  methodName);
        }
        return false;
    }

    /**
     * Generic method that creates instances based on their names and adds them to a Collection
     *
     * Note that the class does its 'best effort' to create an instance and will not fail the method
     * if an instance (or more) was failed to create. This is naturally, a policy we can easily replace
     *
     * @param clazz The class all instances should have
     * @param list The container of instances, this is where we store our instances that we create
     * @param classNames Classes to create.
     * @param <T> The class type to create
     */
    public static <T> void loadComponents(
            Class<T> clazz,
            List<T> list,
            Collection<AddTypeXmlElement> classNames) {
        if (classNames == null) {
            return;
        }

        for (AddTypeXmlElement className : classNames) {
            T initializer = null;

            // If parameters have been provided, we try to load the component with provided parameters map. Otherwise,
            // we fallback to initialize the component with the default ctor.
            if (className.getParameters().size() != 0) {
                initializer = createInstance(className.getType(), clazz, Map.class, className.getData());
            }

            if (initializer == null) {
                initializer = createInstance(className.getType(), clazz);
            }

            if (initializer != null) {
                list.add(initializer);
            }
        }
    }

}
