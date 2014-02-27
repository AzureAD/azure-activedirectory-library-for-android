
package com.microsoft.adal;

import android.content.Intent;

interface IBrokerProxy {
    /**
     * checkd if broker package correct and authenticator valid
     * @return
     */
    boolean canSwitchToBroker();

    /**
     * gets token using authenticator service
     * @param request
     * @return
     */
    String getAuthTokenInBackground(final AuthenticationRequest request);

    /**
     * only gets intent to start from calling app's activity
     * @param request
     * @return
     */
    Intent getIntentForBrokerActivity(final AuthenticationRequest request);
}
