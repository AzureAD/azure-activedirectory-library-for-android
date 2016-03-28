
package com.microsoft.aad.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.test.AndroidTestCase;
import android.util.Base64;
import android.util.Log;

import com.microsoft.aad.adal.AuthenticationConstants;
import com.microsoft.aad.adal.AuthenticationSettings;

public class HttpDialogTests extends AndroidTestCase {

    private static final String TAG = "HttpDialogTests";

    private byte[] testSignature;

    private String testTag;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        getContext().getCacheDir();
        System.setProperty("dexmaker.dexcache", getContext().getCacheDir().getPath());

        // ADAL is set to this signature for now
        PackageInfo info = mContext.getPackageManager().getPackageInfo(
                "com.microsoft.aad.adal.testapp", PackageManager.GET_SIGNATURES);

        // Broker App can be signed with multiple certificates. It will look
        // all of them
        // until it finds the correct one for ADAL broker.
        for (Signature signature : info.signatures) {
            testSignature = signature.toByteArray();
            MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(testSignature);
            testTag = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
            break;
        }
        AuthenticationSettings.INSTANCE.setBrokerSignature(testTag);
        AuthenticationSettings.INSTANCE
                .setBrokerPackageName(AuthenticationConstants.Broker.PACKAGE_NAME);
        Log.d(TAG, "testSignature is set");
    }

    public void testCreateDialogTest() throws NoSuchMethodException, ClassNotFoundException,
            IllegalArgumentException, InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchFieldException {
        String testHost = "http://test.host.com";
        String testRealm = "testRealm";

        Class<?> c = Class.forName("com.microsoft.aad.adal.HttpAuthDialog");
        Constructor<?> constructor = c.getDeclaredConstructor(Context.class, String.class,
                String.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(getContext(), testHost, testRealm);

        Object dialog = ReflectionUtils.getFieldValue(o, "mDialog");
        assertNotNull(dialog);

        String host = (String)ReflectionUtils.getFieldValue(o, "mHost");
        assertEquals(host, testHost);
    }
}
