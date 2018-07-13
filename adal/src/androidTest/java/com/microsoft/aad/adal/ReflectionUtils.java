// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtils {

    public static final String TEST_PACKAGE_NAME = "com.microsoft.aad.adal";

    /**
     * get non public method from class
     * 
     * @param object
     * @param methodName
     * @return
     * @throws IllegalArgumentException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static Method getTestMethod(Object object, final String methodName, Class<?>... paramtypes)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Class<?> c = object.getClass();
        Method m = c.getDeclaredMethod(methodName, paramtypes);
        m.setAccessible(true);
        return m;
    }
    
    public static Method getStaticTestMethod(Class<?> c, final String methodName, Class<?>... paramtypes)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
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
        Object object = m.invoke(instance, (Object[]) null);
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
                //paramTypes[i] = params[i].getClass();
                if (params[i].getClass().getSimpleName().equals("Boolean")) {                    
                    paramTypes[i] = boolean.class;
                } else {
                    paramTypes[i] = params[i].getClass();
                }
            }
        }

        return paramTypes;
    }

}
