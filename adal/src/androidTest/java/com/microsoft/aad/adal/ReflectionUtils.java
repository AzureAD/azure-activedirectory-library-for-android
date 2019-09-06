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
    public static Method getTestMethod(final Object object,
                                       final String methodName,
                                       final Class<?>... paramtypes)
            throws IllegalArgumentException, NoSuchMethodException {
        final Class<?> c = object.getClass();
        final Method m = c.getDeclaredMethod(methodName, paramtypes);
        m.setAccessible(true);
        return m;
    }

    public static Object getFieldValue(final Object object, final String fieldName)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(object);
    }

    public static void setFieldValue(final Object object,
                                     final String fieldName,
                                     final Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        final Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(object, value);
    }

    public static Object getInstance(final String className,
                                     final Object... params)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<?> c = Class.forName(className);
        final Class<?>[] paramTypes = getTypes(params);
        final Constructor<?> constructorParams = c.getDeclaredConstructor(paramTypes);
        constructorParams.setAccessible(true);
        Object o = constructorParams.newInstance(params);
        return o;
    }

    private static Class<?>[] getTypes(final Object... params) {
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
