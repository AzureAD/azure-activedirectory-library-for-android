package com.microsoft.adal.test;

public class Constants {
	public static final String SDK_VERSION = "1.0";
	
	/**
	 * UTF-8 encoding
	 */
	public static final String UTF8_ENCODING = "UTF-8";

	public static final String HEADER_AUTHORIZATION = "Authorization";

	public static final String HEADER_AUTHORIZATION_VALUE_PREFIX = "Bearer ";
	
	
	static final String AUTHORITY_URL = "https://login.windows.net/omercantest.onmicrosoft.com";
	static final String CLIENT_ID = "c4acbce5-b2ed-4dc5-a1b9-c95af96c0277"; // hello
																			// world
																			// app
	static final String RESOURCE_ID = "http://omercantest.onmicrosoft.com/webapi";
	static final String RESOURCE_URL = "http://testapi007.azurewebsites.net/api/";
	static final String TABLE_WORKITEM = "WorkItem";
	static final String REDIRECT_URL = "https://omercantest.onmicrosoft.adal/hello";
	static String USER_HINT = "faruk@omercantest.onmicrosoft.com";
	static final String SERVICE_URL = "http://testapi007.azurewebsites.net";
	
	public static final String SHARED_PREFERENCE_NAME = "com.example.com.test.settings";
	public static final String KEY_NAME_ASK_BROKER_INSTALL = "test.settings.ask.broker";
	public static final String KEY_NAME_CHECK_BROKER = "test.settings.check.broker";	
}
