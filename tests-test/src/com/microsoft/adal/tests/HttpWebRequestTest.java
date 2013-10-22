package com.microsoft.adal.tests;

import java.net.URL;

import com.microsoft.adal.HttpWebRequest;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;

import android.test.AndroidTestCase;

public class HttpWebRequestTest extends AndroidTestCase {

	/**
	 * Webapi to send get, put, post, delete requests and check headers
	 */
	private final static String TEST_WEBAPI_URL = "http://graphtestrun.azurewebsites.net/api/WebRequestTest";
	
	public void testConstructor() {
		try {
			HttpWebRequest request = new HttpWebRequest(null);
			request.sendAsyncGet(null);
			assertFalse("expects failure", true);
		} catch (Exception ex) {
			if (ex instanceof IllegalArgumentException) {
				IllegalArgumentException exc = (IllegalArgumentException) ex;
				assertTrue("Message has url",
						(exc.getMessage().toLowerCase().contains("url")));
			}
		}
	}

	public void testEmptyGetUrl() {
		try {
			HttpWebRequest request = new HttpWebRequest(null);
			request.sendAsyncGet(null);
			assertFalse("expects failure", true);
		} catch (Exception ex) {
			if (ex instanceof IllegalArgumentException) {
				IllegalArgumentException exc = (IllegalArgumentException) ex;
				assertTrue("Message has url",
						(exc.getMessage().toLowerCase().contains("url")));
			}
		}
	}

	public void testEmptyCallback() {
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(
					"http://www.bing.com"));
			request.sendAsyncGet(null);
			assertFalse("expects failure", true);
		} catch (Exception ex) {
			if (ex instanceof IllegalArgumentException) {
				IllegalArgumentException exc = (IllegalArgumentException) ex;
				assertTrue("Message has callback", (exc.getMessage()
						.toLowerCase().contains("callback")));
			}
		}
	}

	public void testConstructorWrongScheme() {
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(
					"ftp://www.microsoft.com"));
			request.sendAsyncGet(null);
			assertFalse("expects failure", true);
		} catch (Exception ex) {
			if (ex instanceof IllegalArgumentException) {
				IllegalArgumentException exc = (IllegalArgumentException) ex;
				assertTrue("Message has url",
						(exc.getMessage().toLowerCase().contains("requesturl")));
			}
		}
	}
	
	public void testGetRequest()
	{
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(TEST_WEBAPI_URL));
			request.getRequestHeaders().put("testabc", "value123");
			request.sendAsyncGet(new HttpWebRequestCallback() {
				
				@Override
				public void onComplete(Exception ex, HttpWebResponse response) {
					assertTrue("exception is null", ex == null);
					assertTrue("status is 200", response.getStatusCode() == 200);
					String responseMsg = new String(response.getBody());
					assertTrue("request header check", responseMsg.contains("testabc-value123")); 
				}
			});
		} catch (Exception ex) {
			assertFalse("not expected", true);
		}
	}
	
	public void testGetWithIdRequest()
	{
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(TEST_WEBAPI_URL+"/1"));
			request.sendAsyncGet(new HttpWebRequestCallback() {
				
				@Override
				public void onComplete(Exception ex, HttpWebResponse response) {
					assertTrue("exception is null", ex == null);
					assertTrue("status is 200", response.getStatusCode() == 200);
					String responseMsg = new String(response.getBody());
					assertTrue("request body check", responseMsg.contains("test get with id")); 
				}
			});
		} catch (Exception ex) {
			assertFalse("not expected", true);
		}
	}
	
	
	
}
