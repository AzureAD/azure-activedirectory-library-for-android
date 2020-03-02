//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.aad.adal;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BrokerEventTest {

    @Test
    public void testProcessEvent() {
        final BrokerEvent event = new BrokerEvent(EventStrings.BROKER_REQUEST_SILENT);
        event.setBrokerAppName("app name");
        event.setBrokerAppVersion("1234");
        event.setBrokerAccountServerStartsBinding();
        event.setBrokerAccountServiceBindingSucceed(true);
        event.setBrokerAccountServiceConnected();

        final Map<String, String> dispatchMap = new HashMap<>();
        event.processEvent(dispatchMap);

        Assert.assertTrue(dispatchMap.containsKey(EventStrings.BROKER_ACCOUNT_SERVICE_BINDING_SUCCEED));
        Assert.assertTrue(dispatchMap.containsKey(EventStrings.BROKER_ACCOUNT_SERVICE_STARTS_BINDING));
        Assert.assertTrue(dispatchMap.containsKey(EventStrings.BROKER_ACCOUNT_SERVICE_CONNECTED));
        Assert.assertTrue(dispatchMap.containsKey(EventStrings.BROKER_APP));
        Assert.assertTrue(dispatchMap.containsKey(EventStrings.BROKER_VERSION));
    }
}
