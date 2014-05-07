
package com.microsoft.aad.adal.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationSettings;

public class ResourceFinderTest extends AndroidTestCase {

    private static final String TAG = "ResourceFinderTest";

    private byte[] testSignature;

    private String testTag;

    public void testResourceFinder() throws IllegalArgumentException, ClassNotFoundException,
            NoSuchMethodException, InstantiationException, IllegalAccessException,
            InvocationTargetException {
        Method m = ReflectionUtils.getStaticTestMethod(
                Class.forName("com.microsoft.aad.adal.ResourceFinder"), "getResourseIdByName",
                String.class, String.class, String.class);

        int id = (Integer)m.invoke(null, "com.microsoft.aad.adal", "layout",
                "activity_authentication");

        assertEquals("Same id", com.microsoft.aad.adal.R.layout.activity_authentication, id);
    }
}
