/*
 * Copyright Microsoft Corporation (c) All Rights Reserved.
 */

package com.microsoft.adal;

/**
 * MOVED from Hervey's code
 * Interface for callbacks used in asynchronous HttpWebRequest operations
 */
interface HttpWebRequestCallback
{
    void onComplete( Exception ex, HttpWebResponse response );
}
