package com.microsoft.adal;

import android.net.Uri;

public class AuthenticationParams {

    public String getAuthority(){
        throw new UnsupportedOperationException();
    }
    
    public String getResource(){
        throw new UnsupportedOperationException();
    }
        
    public static AuthenticationParams createFromResourceUrl(Uri resourceUrl){
        throw new UnsupportedOperationException();
    }
    
    public static AuthenticationParams createFromResponseAuthenticateHeader(String authenticateHeader){
        throw new UnsupportedOperationException();
    }
}
