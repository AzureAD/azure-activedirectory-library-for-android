
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
     * test constructor to make sure authority parameter is set
     */
    public void testConstructor()
    {
        String authority = "authority";
        AuthenticationContext context = new AuthenticationContext(getContext(), authority, false);
        assertSame(authority, context.getAuthority());
    }
}
