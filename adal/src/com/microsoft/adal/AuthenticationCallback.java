/**
 * Copyright (c) Microsoft Corporation. All rights reserved. 
 */

package com.microsoft.adal;

/**
 * Callback to use with token request. User implements this callback to use
 * result in their context.
 * 
 * @author omercan
 */
public interface AuthenticationCallback {

    /**
     * This will have the token info.
     * 
     * @param result
     */
    public void onSuccess(AuthenticationResult result);

    /**
     * Sends error information. This can be user related error or server error.
     * Cancellation error is AuthenticationCancelError.
     * 
     * @param exc
     */
    public void onError(Exception exc);
}
