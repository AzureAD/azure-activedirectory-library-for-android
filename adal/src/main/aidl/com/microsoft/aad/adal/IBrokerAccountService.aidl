package com.microsoft.aad.adal;

/**
 * Broker Account service APIs provided by the broker app. Those APIs will be responsible for interacting with the
 * account manager API. Calling app does not need to request for contacts permission if the broker installed on the
 * device has the support for the bound service.
 */
interface IBrokerAccountService {

    Bundle getBrokerUsers();
    
    Bundle acquireTokenSilently(in Map requestParameters);
    
    Intent getIntentForInteractiveRequest();

    void removeAccounts();
}