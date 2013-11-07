
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

    public static Object getFieldValue(Object object, String fieldName) throws NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException {
        Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(object);
    }
    
    public static void setFieldValue(Object object, String fieldName, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException{
        Field f = object.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(object, value);
    }
}
