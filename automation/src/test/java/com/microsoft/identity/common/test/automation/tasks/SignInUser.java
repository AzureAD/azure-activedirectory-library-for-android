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

package com.microsoft.identity.common.test.automation.tasks;


import net.serenitybdd.screenplay.Task;

public abstract class SignInUser implements Task {
    public static SignInUser GetSignInUserByFederationProvider(String federationProvider) {
        SignInUser signInUserTask = null;

        switch (federationProvider) {
            case "ADFSv2":
                signInUserTask = new SignInUserADFSv2();
                break;
            case "ADFSv3":
                signInUserTask = new SignInUserADFSv3();
                break;
            case "ADFSv4":
                signInUserTask = new SignInUserADFSv4();
                break;
            case "PingFederate V8.3":
                signInUserTask = new SignInUserPing();
                break;
            case "Shibboleth":
                signInUserTask = new SignInUserShibboleth();
                break;
            default:
                signInUserTask = new SignInUserADFSv2();
        }

        return signInUserTask;
    }
}
