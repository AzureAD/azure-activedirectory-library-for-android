package com.microsoft.adal.tests;

import java.util.Iterator;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationResult;
import com.microsoft.adal.TokenCache;

public class TokenCacheTests extends AndroidTestCase{

	public void testIterableInterface()
	{
		AuthenticationResult dummyResult = new AuthenticationResult("errCode123", "description");
		TokenCache cache = new TokenCache(this.mContext);
		cache.removeAll();
		cache.putResult("test", dummyResult);
		
	    Iterator<AuthenticationResult> results = cache.getIterator();
	    while(results.hasNext())
	    {
	    	AuthenticationResult result = results.next();
	    	
	    	assertFalse("One item", results.hasNext());
	    	assertTrue("Same obj", result.getErrorCode() == "errCode123");
	    }
	}
}
