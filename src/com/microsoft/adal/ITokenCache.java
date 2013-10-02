package com.microsoft.adal;

import java.util.HashMap;


/**
 * Stores token related info such as access token, refresh token, and expiration
 * @author omercan
 *
 */
public interface ITokenCache {
	public AuthenticationResult getResult( String key );
    public boolean putResult( String key, AuthenticationResult result );
    public boolean removeResult( String key );
    public boolean removeAll();
    public HashMap<String, AuthenticationResult> getAllResults();
}
