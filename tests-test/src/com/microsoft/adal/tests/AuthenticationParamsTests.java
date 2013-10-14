package com.microsoft.adal.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.test.AndroidTestCase;

import com.microsoft.adal.AuthenticationParameters;
import com.microsoft.adal.AuthenticationParameters.AuthenticationParamCallback;
import com.microsoft.adal.ErrorMessages;
import com.microsoft.adal.HttpWebResponse;

public class AuthenticationParamsTests extends AndroidTestCase {

	public void testGetAuthority() {
		AuthenticationParameters param = new AuthenticationParameters();
		assertTrue("authority should be null", param.getAuthority() == null);
	}

	public void testGetResource() {
		AuthenticationParameters param = new AuthenticationParameters();
		assertTrue("resource should be null", param.getResource() == null);
	}

	public void testCreateFromResourceUrlInvalidFormat() {

		try {
			AuthenticationParameters.createFromResourceUrl(new URL(
					"http://www.bing.com"), new AuthenticationParamCallback() {

				@Override
				public void onCompleted(Exception exception,
						AuthenticationParameters param) {
					assertNotNull(exception);
					assertNull(param);
					assertTrue(
							"Check header exception",
							exception.getMessage() == ErrorMessages.AUTH_HEADER_WRONG_STATUS);
				}
			});

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

	}

	public void testParseResponseWrongStatus() {
		// send wrong status
		
		Method m = null;
		try {
			m = AuthenticationParameters.class
					.getDeclaredMethod("parseResponse");
		} catch (NoSuchMethodException e) {
			assertTrue("parseResponse is not found", false);
		}

		m.setAccessible(true);
		try {
			m.invoke(null, new HttpWebResponse(200, null, null), new AuthenticationParamCallback() {

				@Override
				public void onCompleted(Exception exception,
						AuthenticationParameters param) {
					assertNotNull(exception);
					assertNull(param);
					assertTrue(
							"Check header exception",
							exception.getMessage() == ErrorMessages.AUTH_HEADER_WRONG_STATUS);
				}
			});
			
			// correct status
			m.invoke(null, new HttpWebResponse(401, null, null), new AuthenticationParamCallback() {

				@Override
				public void onCompleted(Exception exception,
						AuthenticationParameters param) {
					assertNotNull(exception);
					assertNull(param);
					assertTrue(
							"Check header exception",
							exception.getMessage() == ErrorMessages.AUTH_HEADER_MISSING);
				}
			});
			
			// correct status, but incorrect header
			m.invoke(null, new HttpWebResponse(401, null , getInvalidHeader("WWW-Authenticate","v")), new AuthenticationParamCallback() {

				@Override
				public void onCompleted(Exception exception,
						AuthenticationParameters param) {
					assertNotNull(exception);
					assertNull(param);
					assertTrue(
							"Check header exception",
							exception.getMessage() == ErrorMessages.AUTH_HEADER_INVALID_FORMAT);
				}
			});
			
			// correct status, but incorrect authorization param
			m.invoke(null, new HttpWebResponse(401, null , getInvalidHeader("WWW-Authenticate","Bearer nonsense")), new AuthenticationParamCallback() {

				@Override
				public void onCompleted(Exception exception,
						AuthenticationParameters param) {
					assertNotNull(exception);
					assertNull(param);
					assertTrue(
							"Check header exception",
							exception.getMessage() == ErrorMessages.AUTH_HEADER_INVALID_FORMAT);
				}
			});

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue("parseResponse is not found", false);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue("parseResponse is not found", false);
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assertTrue("parseResponse is not found", false);
		}
	}

	public void testCreateFromResponseAuthenticateHeader() {

	}
	
	
	
	
	
	
	private HashMap<String, List<String>> getInvalidHeader(String key, String value)
	{
		HashMap<String, List<String>> dummy = new HashMap<String, List<String>>();
		dummy.put(key, Arrays.asList(value, "s2", "s3"));
		return dummy;
	}
};