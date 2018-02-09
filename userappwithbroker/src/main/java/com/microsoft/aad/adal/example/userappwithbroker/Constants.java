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

/**
 * Constants file.
 */

public class Constants {
    enum AuthorityType {
        AAD_COMMON("https://login.microsoftonline.com/common"),
        AAD_MSDEVEX("https://login.microsoftonline.com/msdevex.onmicrosoft.com"),
        AAD_GUEST("https://login.microsoftonline.com/nomfaad.onmicrosoft.com");

        private final String text;
        AuthorityType(String s) {
            text = s;
        }
        public String getText() {
            return text;
        }
    }

    enum DataProfile {
        SHAREPOINT("https://msdevex-my.sharepoint.com"),
        GRAPH("https://graph.windows.net"),
        OFFICE_ONEDRIVE("https://api.office.com/discovery"),
        SIMPLE("00000002-0000-0000-c000-000000000000");

        private final String text;
        DataProfile(String s) {
            text = s;
        }
        public String getText() {
            return text;
        }
    }


    enum ClientId {
        MSDEVEX("b92e0ba5-f86e-4411-8e18-6b5f928d968a"),
        ONEDRIVE("af124e86-4e96-495a-b70a-90f90ab96707"),
        OFFICE("d3590ed6-52b3-4102-aeff-aad2292ab01c"),
        APPCHECK2_BF("f5d01c1c-abe6-4207-ae2d-5bc9af251724"),
        LAB4("4b0db8c2-9f26-4417-8bde-3f0e3656f8e0"),
        GUESTCLIENT("ea5c8087-2476-489c-ae03-ad44a2ac399d");

        private final String text;
        ClientId(String s) {
            text = s;
        }
        public String getText() {
            return text;
        }
    }

    enum RedirectUri {
        Regular("msauth://com.microsoft.aad.adal.userappwithbroker/QK0hWtPIQviyU3IX8AhunaS0IY4%3D"),
        Broker("urn:ietf:wg:oauth:2.0:oob");

        private final String text;
        RedirectUri(String s) {
            text = s;
        }
        public String getText() {
            return text;
        }
    }

}
