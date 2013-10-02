package com.microsoft.adal.test;

import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationResult.AuthenticationStatus;

import android.test.AndroidTestCase;

public class TestAuthenticationResult extends AndroidTestCase {

	public void testCtor() {

		AuthenticationResult result = new AuthenticationResult();
		assertTrue(result.getAccessToken() == null);
		assertTrue(result.getRefreshToken() == null);
		assertTrue(!result.IsBroadRefreshToken());
		assertTrue(result.getStatus() == AuthenticationStatus.Failed);

	}
}
