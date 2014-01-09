package com.microsoft.adal.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

public class StorageHelperTests extends AndroidTestCase {

    private static final String TAG = "StorageHelperTests";

    public void testEncryptDecrypt() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        String clearText = "SomeValue1234";
        Log.d(TAG, "short text testEncryptDecrypt");
        Object storageHelper = getStorageHelper();
        Method mEncrypt = ReflectionUtils.getTestMethod(storageHelper, "encrypt", String.class);
        Method mDecrypt = ReflectionUtils.getTestMethod(storageHelper, "decrypt", String.class);
        String encrypted = (String)mEncrypt.invoke(storageHelper, clearText);
        assertNotNull("encrypted string is not null", encrypted);
        assertFalse("encrypted string is not same as cleartex", encrypted.equals(clearText));
        String decrypted = (String)mDecrypt.invoke(storageHelper, encrypted);
        assertEquals("Same as initial text", clearText, decrypted);
        Log.d(TAG, "Long text testEncryptDecrypt");
    }

    public void testFixedPassword() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Log.d(TAG, "short text testEncryptDecrypt");
        Object storageHelper = getStorageHelper();
        Method m = ReflectionUtils.getTestMethod(storageHelper, "getPassword");

        byte[] passwordBytes = (byte[])m.invoke(storageHelper);

        assertNotNull("encrypted string is not null", passwordBytes);
        assertTrue("contains fixed info",
                new String(passwordBytes).contains("B58AD341-0D4B-46B8-A81B-552B86051BDD"));

    }

    private Object getStorageHelper() throws NoSuchMethodException, ClassNotFoundException,
            InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> constructorParams = Class.forName("com.microsoft.adal.StorageHelper")
                .getDeclaredConstructor(Context.class);
        constructorParams.setAccessible(true);
        Object storageHelper = constructorParams.newInstance(getContext());
        return storageHelper;
    }
}