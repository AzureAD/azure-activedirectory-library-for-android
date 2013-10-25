/*
 * Copyright Microsoft Corporation (c) All Rights Reserved.
 */

package com.microsoft.adal;

public interface HttpWebRequestCallback {
	void onComplete(HttpWebResponse response);
}
