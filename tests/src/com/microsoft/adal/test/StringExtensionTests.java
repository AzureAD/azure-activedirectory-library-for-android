
package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import junit.framework.Assert;

import android.test.AndroidTestCase;

/**
 * StringExtensions class has helper methods and it is not public
 * 
 * @author omercan
 */
public class StringExtensionTests extends AndroidTestHelper {
    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsNullOrBlankNotEmpty() {
        final String methodName = "IsNullOrBlank";
        try {
            Object foo = getStringExtensionInstance();
            Method m = getTestMethod(foo, methodName);
            boolean result = (Boolean) m.invoke(foo, "non-empty");
            assertFalse("not empty", result);
        } catch (Exception ex) {
            Assert.fail("Dont expect exception");
        }
    }

    public void testIsNullOrBlankEmpty() {
        final String methodName = "IsNullOrBlank";
        try {
            Object foo = getStringExtensionInstance();
            Method m = getTestMethod(foo, methodName);
            boolean result = (Boolean) m.invoke(foo, "");
            assertTrue("empty", result);

            result = (Boolean) m.invoke(foo, "  ");
            assertTrue("empty", result);

            result = (Boolean) m.invoke(foo, "          ");
            assertTrue("empty", result);

        } catch (Exception ex) {
            Assert.fail("Dont expect exception");
        }
    }

    public void testURLFormEncodeDecode() {
        final String methodName = "URLFormEncode";
        try {
            Object foo = getStringExtensionInstance();
            Method m = getTestMethod(foo, methodName);
            Method decodeMethod = getTestMethod(foo, "URLFormDecode");

            String result = (String) m.invoke(foo,
                    "https://login.windows.net/aaltests.onmicrosoft.com/");
            assertEquals("https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F", result);

            result = (String) decodeMethod.invoke(foo,
                    "https%3A%2F%2Flogin.windows.net%2Faaltests.onmicrosoft.com%2F");
            assertEquals("https://login.windows.net/aaltests.onmicrosoft.com/", result);

            result = (String) m.invoke(foo, "abc d1234567890-");
            assertEquals("abc+d1234567890-", result);

            result = (String) decodeMethod.invoke(foo, "abc+d1234567890-");
            assertEquals("abc d1234567890-", result);

            String longString = "asdfk+j0a-=skjwe43;1l234 1#$!$#%345903485qrq@#$!@#$!(rekr341!#$%Ekfaآزمايشsdsdfsddfdgsfgjsglk==CVADS";
            result = (String) m.invoke(foo, longString);

            String decodeResult = (String) decodeMethod.invoke(foo, result);
            assertEquals(longString, decodeResult);

        } catch (Exception ex) {
            Assert.fail("Dont expect exception");
        }
    }

    private Method getTestMethod(Object foo, final String methodName)
            throws IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException
    {

        Class<?> c = foo.getClass();
        Method m = c.getDeclaredMethod(methodName, String.class);
        m.setAccessible(true);
        return m;
    }

    private Object getStringExtensionInstance() throws ClassNotFoundException,
            NoSuchMethodException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException
    {
        // full package name
        Class<?> c;

        c = Class.forName("com.microsoft.adal.StringExtensions");

        // getConstructor() returns only public constructors,

        Constructor<?> constructor = c.getDeclaredConstructor();

        constructor.setAccessible(true);
        Object o = constructor.newInstance(null);

        return o;
    }
}
