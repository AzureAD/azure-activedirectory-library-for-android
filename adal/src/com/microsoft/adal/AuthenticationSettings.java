package com.microsoft.adal;

/**
 * Settings to be used in AuthenticationContext
 * @author omercan
 *
 */
public enum AuthenticationSettings {
    INSTANCE;    

    private String mKeywordAuthorize = "/oauth2/authorize";
    private String mKeywordToken = "/oauth2/token";
    
    /**
     * default is /oauth2/token
     * @param keyword
     */
    public void setTokenEndpointKeyword(String keyword){
        mKeywordToken = keyword;
    }
    
    public String getTokenEndpointKeyword(){
        return mKeywordToken;
    }
    
    public void setAuthorizeEndpointKeyword(String keyword){
        mKeywordAuthorize = keyword;
    }
    
    public String getAuthorizeEndpointKeyword(){
        return mKeywordAuthorize;
    }
}
