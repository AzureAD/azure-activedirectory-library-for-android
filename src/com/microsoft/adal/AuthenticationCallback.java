/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */
package com.microsoft.adal;

/**
 * Callback to use with token request. User implements this callback to use result in their context.
 * @author omercan
 *
 */
public interface AuthenticationCallback {
	 
		/**
		 * This will have the token info.
		 * @param result
		 */
		public void onCompleted(AuthenticationResult result);
		
		/**
		 * Send error information. This can be user related error or server error. 
		 * Some cases returns exception and some cases server returns error object such as in the Oauth response.
		 * This needs to be displayed at the user's screens. 
		 * @param exc
		 */
		public void onError(Exception exc);
		
		/**
		 * User cancelled the dialog or similar
		 */
		public void onCancelled();
	}
 
