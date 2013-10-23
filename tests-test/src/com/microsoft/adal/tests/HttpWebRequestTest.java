package com.microsoft.adal.tests;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.adal.AuthenticationConstants;
import com.microsoft.adal.HttpWebRequest;
import com.microsoft.adal.HttpWebRequestCallback;
import com.microsoft.adal.HttpWebResponse;

import android.test.AndroidTestCase;

public class HttpWebRequestTest extends AndroidTestCase {

	/**
	 * Webapi to send get, put, post, delete requests and check headers
	 * This is deployed at azure websites. Simple WebAPI to verify that requests are going through and parsed correctly.
	 */
	private final static String TEST_WEBAPI_URL = "http://graphtestrun.azurewebsites.net/api/WebRequestTest";
	private final static int REQUEST_TIME_OUT = 20000; // miliseconds

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

	public void testGetRequest() {
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			HttpWebRequest request = new HttpWebRequest(
					new URL(TEST_WEBAPI_URL));
			request.getRequestHeaders().put("testabc", "value123");
			request.sendAsyncGet(new HttpWebRequestCallback() {

				@Override
				public void onComplete(Exception ex, HttpWebResponse response) {
					assertTrue("exception is null", ex == null);
					assertTrue("status is 200", response.getStatusCode() == 200);
					String responseMsg = new String(response.getBody());
					assertTrue("request header check",
							responseMsg.contains("testabc-value123"));
					signal.countDown();
				}
			});
		} catch (Exception ex) {
			assertFalse("not expected", true);
			signal.countDown();
		}

		try {
			signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			assertFalse("InterruptedException is not expected", true);
		}
	}

	public void testGetWithIdRequest() {
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(TEST_WEBAPI_URL
					+ "/1"));
			request.sendAsyncGet(new HttpWebRequestCallback() {

				@Override
				public void onComplete(Exception ex, HttpWebResponse response) {
					assertTrue("exception is null", ex == null);
					assertTrue("status is 200", response.getStatusCode() == 200);
					String responseMsg = new String(response.getBody());
					assertTrue("request body check",
							responseMsg.contains("test get with id"));
					signal.countDown();
				}
			});
		} catch (Exception ex) {
			assertFalse("not expected", true);
			signal.countDown();
		}

		try {
			signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			assertFalse("InterruptedException is not expected", true);
		}
	}

	public void testPostRequest() {
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			HttpWebRequest request = new HttpWebRequest(
					new URL(TEST_WEBAPI_URL));
			final TestMessage message = new TestMessage("messagetest", "12345");
			String json = new Gson().toJson(message);

			request.sendAsyncPost(
					json.getBytes(AuthenticationConstants.ENCODING_UTF8),
					"application/json", new HttpWebRequestCallback() {

						@Override
						public void onComplete(Exception ex,
								HttpWebResponse response) {
							assertTrue("exception is null", ex == null);
							assertTrue("status is 200",
									response.getStatusCode() == 200);
							String responseMsg = new String(response.getBody());
							assertTrue("request body check", responseMsg
									.contains(message.getAccessToken()
											+ message.getUserName()));
							signal.countDown();
						}
					});
		} catch (Exception ex) {
			assertFalse("not expected", true);
			signal.countDown();
		}

		try {
			signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			assertFalse("InterruptedException is not expected", true);
		}
	}

	public void testDeleteRequest() {
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(TEST_WEBAPI_URL
					+ "/1"));
			request.sendAsyncDelete(new HttpWebRequestCallback() {
				@Override
				public void onComplete(Exception ex, HttpWebResponse response) {
					assertTrue("exception is null", ex == null);
					assertTrue("status is 204", response.getStatusCode() == 204);
					signal.countDown();
				}
			});
		} catch (Exception ex) {
			assertFalse("not expected", true);
			signal.countDown();
		}

		try {
			signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			assertFalse("InterruptedException is not expected", true);
		}
	}

	/**
	 * test update call
	 */
	public void testPutRequest() {
		final CountDownLatch signal = new CountDownLatch(1);
		try {
			HttpWebRequest request = new HttpWebRequest(new URL(TEST_WEBAPI_URL
					+ "/147"));
			final TestMessage message = new TestMessage("putrequest", "342");
			String json = new Gson().toJson(message);

			request.sendAsyncPut(
					json.getBytes(AuthenticationConstants.ENCODING_UTF8),
					"application/json", new HttpWebRequestCallback() {

						@Override
						public void onComplete(Exception ex,
								HttpWebResponse response) {
							assertTrue("exception is null", ex == null);
							assertTrue("status is 200",
									response.getStatusCode() == 200);
							String responseMsg = new String(response.getBody());
							assertTrue(
									"request body check",
									responseMsg.contains("147"
											+ message.getAccessToken()
											+ message.getUserName()));
							signal.countDown();
						}
					});
		} catch (Exception ex) {
			assertFalse("not expected", true);
			signal.countDown();
		}

		try {
			signal.await(REQUEST_TIME_OUT, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			assertFalse("InterruptedException is not expected", true);
		}
	}

	class TestMessage {
		@com.google.gson.annotations.SerializedName("AccessToken")
		private String mAccessToken;

		@com.google.gson.annotations.SerializedName("UserName")
		private String mUserName;

		public TestMessage(String token, String name) {
			mAccessToken = token;
			mUserName = name;
		}

		public String getAccessToken() {
			return mAccessToken;
		}

		public void setAccessToken(String mAccessToken) {
			this.mAccessToken = mAccessToken;
		}

		public String getUserName() {
			return mUserName;
		}

		public void setUserName(String mUserName) {
			this.mUserName = mUserName;
		}

	}
}
