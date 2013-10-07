package com.microsoft.adal.test;


import java.util.Calendar;
import java.util.Date;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.AuthenticationResult.AuthenticationStatus;

public class AuthenticationResultTests extends AndroidTestCase {

	public void testCtor() {

		AuthenticationResult result = new AuthenticationResult();
		assertTrue(result.getAccessToken() == null);
		assertTrue(result.getRefreshToken() == null);
		assertTrue(!result.IsBroadRefreshToken());
		assertTrue(result.getStatus() == AuthenticationStatus.Failed);

		result = new AuthenticationResult("errorCode", "errDescription");
		assertEquals("errorCode", result.getErrorCode());
		assertEquals("errDescription", result.getErrorDescription());
		
	}
	
	public void testStatus()
	{
		AuthenticationResult result = new AuthenticationResult();
		result.setAccessToken("test");
		assertTrue(result.getStatus() == AuthenticationStatus.Succeeded);
	}
	
	public void testIsExpired()
	{
		AuthenticationResult result = new AuthenticationResult();
		Date expiredDate = new Date();
		Calendar timeAhead = Calendar.getInstance();
	    timeAhead.roll(Calendar.MINUTE, -5);
		result.setExpires(timeAhead.getTime());
		
		assertTrue(result.isExpired());
		
		timeAhead.roll(Calendar.MINUTE, 15);
		result.setExpires(timeAhead.getTime());
		
		assertFalse(result.isExpired());
	}
}
