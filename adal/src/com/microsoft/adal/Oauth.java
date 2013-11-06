
package com.microsoft.adal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.util.Base64;

/**
 * not part of API
 * 
 * @author omercan
 */
class Oauth {

    private AuthenticationRequest mRequest;

    /**
     * RequestAuthEndpoint to append in authority url
     */
    private final static String AUTH_ENDPOINT_APPEND = "/oauth2/authorize";

    /**
     * RequesttokenEndpoint to append in authority url
     */
    private final static String TOKEN_ENDPOINT_APPEND = "/oauth2/token";

    Oauth(AuthenticationRequest request) {
        mRequest = request;
    }

    public String getAuthorizationEndpoint() {
        return mRequest.getAuthority() + AUTH_ENDPOINT_APPEND;
    }

    public String getTokenEndpoint() {
        return mRequest.getAuthority() + TOKEN_ENDPOINT_APPEND;
    }

    public String getCodeRequestUrl() throws UnsupportedEncodingException {

        String requestUrl = String
                .format("%s?response_type=%s&client_id=%s&resource=%s&redirect_uri=%s&state=%s",
                        mRequest.getAuthority() + AUTH_ENDPOINT_APPEND,
                        AuthenticationConstants.OAuth2.CODE, mRequest.getClientId(), URLEncoder
                                .encode(mRequest.getResource(),
                                        AuthenticationConstants.ENCODING_UTF8), URLEncoder.encode(
                                mRequest.getRedirectUri(), AuthenticationConstants.ENCODING_UTF8),
                        encodeProtocolState());

        if (mRequest.getLoginHint() != null && !mRequest.getLoginHint().isEmpty()) {
            requestUrl = String.format("%s&%s=%s", requestUrl,
                    AuthenticationConstants.AAD.LOGIN_HINT, URLEncoder.encode(
                            mRequest.getLoginHint(), AuthenticationConstants.ENCODING_UTF8));
        }

        return requestUrl;
    }

    public static String decodeProtocolState(String encodedState) {
        byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);

        return new String(stateBytes);
    }

    private String encodeProtocolState() {
        String state = String.format("a=%s&r=%s", mRequest.getAuthority(), mRequest.getResource());
        return Base64.encodeToString(state.getBytes(), Base64.NO_PADDING | Base64.URL_SAFE);
    }
}
