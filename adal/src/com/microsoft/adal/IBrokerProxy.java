package com.microsoft.adal;

import android.content.Intent;


interface IBrokerProxy {
    boolean canSwitchToBroker();
    String getAuthTokenInBackground(final AuthenticationRequest request);
    Intent getIntentForBrokerActivity(final AuthenticationRequest request);
}
