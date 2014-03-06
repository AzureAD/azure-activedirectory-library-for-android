// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

    public final static String TEST_PACKAGE_NAME = "com.microsoft.adal";

    /**
     * get non public method from class
     * 
     * @param foo
     * @param methodName
     * @return
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Method getTestMethod(Object foo, final String methodName, Class<?>... paramtypes)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> c = foo.getClass();
        Method m = c.getDeclaredMethod(methodName, paramtypes);
        m.setAccessible(true);
        return m;
    }

    /**
     * get non public instance default constructor for testing
     * 
     * @param name
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Object getNonPublicInstance(String name) throws ClassNotFoundException,
            NoSuchMethodException, IllegalArgumentException, InstantiationException,
            IllegalAccessException, InvocationTargetException {
        // full package name
        Class<?> c = Class.forName(name);

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        return constructor.newInstance(null);
    }

    public static Object getFieldValue(Object object, String fieldName)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(object);
    }

    public static void setFieldValue(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(object, value);
    }

    public static <T> T getterValue(Class<T> clazz, Object instance, String methodName)
            throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Method m = instance.getClass().getDeclaredMethod(methodName);
        Object object = m.invoke(instance, (Object[])null);
        return clazz.cast(object);
    }

    public static void setterValue(Object authenticationRequest, String methodName, Object param)
            throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException,
            InvocationTargetException {
        Method[] methods = authenticationRequest.getClass().getDeclaredMethods();
        Method targetMethod = null;
        // target only name for setters
        for (Method m : methods) {
            if (m.getName().equals(methodName)) {
                targetMethod = m;
                break;
            }
        }

        if (targetMethod != null) {
            targetMethod.invoke(authenticationRequest, param);
        } else {
            throw new NoSuchMethodException();
        }
    }

    public static Object getInstance(String className, Object... params)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> c = Class.forName(className);
        Class<?>[] paramTypes = getTypes(params);
        Constructor<?> constructorParams = c.getDeclaredConstructor(paramTypes);
        constructorParams.setAccessible(true);
        Object o = constructorParams.newInstance(params);
        return o;
    }

    private static Class<?>[] getTypes(Object... params) {
        Class<?>[] paramTypes = null;
        if (params != null) {
            paramTypes = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++) {
                paramTypes[i] = params[i].getClass();
            }
        }

        return paramTypes;
    }

}
