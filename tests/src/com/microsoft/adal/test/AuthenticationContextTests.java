
package com.microsoft.adal.test;

import com.microsoft.adal.AuthenticationContext;

import android.test.AndroidTestCase;

public class AuthenticationContextTests extends AndroidTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * dummy initial test
     */
    public void testConstructor()
    {
        String authority = "authority";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false);
        assertSame(authority, context.getAuthority());
    }
}
