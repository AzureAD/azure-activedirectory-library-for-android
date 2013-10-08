package com.microsoft.adal.test;

import android.net.Uri;
import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationParams;

public class AuthenticationParamsTests extends AndroidTestCase {

	public void testGetAuthority() {
		AuthenticationParams param = new AuthenticationParams();
		assertTrue("authority should be null", param.getAuthority() == null);
	}

	public void testGetResource() {
		AuthenticationParams param = new AuthenticationParams();
		assertTrue("resource should be null", param.getResource() == null);
	}

	public void testCreateFromResourceUrl() {
		Uri myUri = Uri.parse("http://www.bing.com");
		
		AuthenticationParams param = AuthenticationParams.createFromResourceUrl(myUri);
		assertTrue("authority should be null", param.getAuthority() == null);
	}

	public void testCreateFromResponseAuthenticateHeader() {

	}
}
