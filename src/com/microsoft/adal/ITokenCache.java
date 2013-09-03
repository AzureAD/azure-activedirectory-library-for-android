package com.microsoft.adal;

import java.io.Serializable;

/**
 * Stores token related info such as access token, refresh token, and expiration
 * @author omercan
 *
 */
public interface ITokenCache {
	public AuthenticationResult getResult( String key );
    public void putResult( String key, AuthenticationResult result );
    public void removeResult( String key );
    public void removeAll();
}
