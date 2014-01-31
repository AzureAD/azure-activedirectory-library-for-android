
package com.microsoft.adal;

class AuthenticationRequestState {
    public AuthenticationRequestState(int requestCallbackId, AuthenticationRequest request,
            AuthenticationCallback<AuthenticationResult> delegate) {
        mRequestId = requestCallbackId;
        mDelagete = delegate;
        mRequest = request;
    }

    public int mRequestId = 0;

    public AuthenticationCallback<AuthenticationResult> mDelagete = null;

    public boolean mCancelled = false;

    public AuthenticationRequest mRequest = null;
}
