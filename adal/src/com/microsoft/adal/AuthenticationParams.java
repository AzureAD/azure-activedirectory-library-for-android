package com.microsoft.adal;

import android.net.Uri;

public class AuthenticationParams {

    private String mAuthority;
    private String mResource;
    
    public String getAuthority(){
        return mAuthority;
    }
    
    public String getResource(){
        return mResource;
    }
        
    public static AuthenticationParams createFromResourceUrl(Uri resourceUrl){
        throw new UnsupportedOperationException();
    }
    
    public static AuthenticationParams createFromResponseAuthenticateHeader(String authenticateHeader){
        throw new UnsupportedOperationException();
    }
}
