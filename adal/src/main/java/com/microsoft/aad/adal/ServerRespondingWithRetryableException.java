// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.adal;

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;

/**
 * server responding with retryable error.
 */
class ServerRespondingWithRetryableException extends AuthenticationException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new ServerRespondingWithRetryableException with message.
     *
     * @param msg Detailed message for the reason causing the exception.
     */
    ServerRespondingWithRetryableException(String msg) {
        super(null, msg);
    }

    /**
     * Constructs a new ServerRespondingWithRetryableException with message and the cause exception.
     *
     * @param msg       Detailed message for the reason causing the exception.
     * @param throwable {@link Throwable}
     */
    ServerRespondingWithRetryableException(String msg, Throwable throwable) {
        super(null, msg, throwable);
    }

    /**
     * Constructs a new ServerRespondingWithRetryableException with message and the http web response.
     *
     * @param msg      Detailed message for the reason causing the exception.
     * @param response {@Link HttpWebResponse}
     */
    ServerRespondingWithRetryableException(String msg, HttpWebResponse response) {
        super(null, msg, response);
    }
}
