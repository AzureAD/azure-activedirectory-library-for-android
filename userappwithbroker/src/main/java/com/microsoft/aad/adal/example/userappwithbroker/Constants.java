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

package com.microsoft.aad.adal.example.userappwithbroker;

public class Constants {
    enum AuthorityType {
        AAD_COMMON("https://login.microsoftonline.com/common"),
        MOONCAKE_COMMON("https://login.chinacloudapi.cn/common"),
        AAD_WINDOWS_NET("https://login.windows.net/common");

        private final String text;

        AuthorityType(String s) {
            text = s;
        }

        public String getText() {
            return text;
        }
    }

    enum DataProfile {
        GRAPH("https://graph.windows.net"),
        MOONCAKE_GRAPH("https://graph.chinacloudapi.cn"),
        SHAREPOINT("00000003-0000-0ff1-ce00-000000000000");

        private final String text;

        DataProfile(String s) {
            text = s;
        }

        public String getText() {
            return text;
        }
    }


    enum ClientId {
        ADFSV4("4b0db8c2-9f26-4417-8bde-3f0e3656f8e0"),
        MOONCAKE("920cd637-4ca3-496b-8a6a-f7c6ca1b2b82");

        private final String text;

        ClientId(String s) {
            text = s;
        }

        public String getText() {
            return text;
        }
    }

    enum RedirectUri {
        Regular(BuildConfig.REGULAR_REDIDRECT_URI),
        Regular2("msauth://com.microsoft.aad.adal.userappwithbroker/L8kGVGYgNOaxbhn9Y7vR%2F6LIEG8%3D"),
        Mooncake("msauth://com.msft.identity.client.sample.local/1wIqXSqBj7w%2Bh11ZifsnqwgyKrY%3D"),
        Broker("urn:ietf:wg:oauth:2.0:oob"),
        LABS("msauth://com.microsoft.aad.adal.userappwithbroker/2%2BQqCWt1ilKg0IrfKT6CkdMpPqk%3D"),
        LABSDEBUG("msauth://com.microsoft.aad.adal.userappwithbroker/1wIqXSqBj7w%2bh11ZifsnqwgyKrY%3d");

        private final String text;

        RedirectUri(String s) {
            text = s;
        }

        public String getText() {
            return text;
        }
    }
}
