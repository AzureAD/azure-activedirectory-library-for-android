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

import com.google.gson.Gson;

import java.util.UUID;

/**
 * Creates correlatable Requests to HTTP accessible resources.
 */
abstract class AbstractMetadataRequestor<MetadataType, MetadataRequestOptions> {

    /**
     * Used to handle network requests.
     */
    private final IWebRequestHandler mWebrequestHandler;

    private UUID mCorrelationId;

    /**
     * Response parser.
     */
    private Gson mGson;

    /**
     * Constructs a new AbstractorRequestor.
     */
    AbstractMetadataRequestor() {
        mWebrequestHandler = new WebRequestHandler();
    }

    public final void setCorrelationId(final UUID requestCorrelationId) {
        mCorrelationId = requestCorrelationId;
    }

    public final UUID getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Gets the thread-safe lazy-initialized parser for JSON.
     *
     * @return the gson instance
     */
    synchronized Gson parser() {
        if (mGson == null) {
            mGson = new Gson();
        }

        return mGson;
    }

    /**
     * Gets the IWebRequestHandler used to make requests.
     *
     * @return the IWebRequestHandler
     */
    IWebRequestHandler getWebrequestHandler() {
        return mWebrequestHandler;
    }

    /**
     * Requests the specified {@link MetadataType}.
     *
     * @param options parameters used for this request
     * @return the metadata
     * @throws Exception if the metadata fails to load/deserialize
     */
    abstract MetadataType requestMetadata(MetadataRequestOptions options) throws Exception;

    /**
     * Deserializes {@link HttpWebResponse} objects into the specified {@link MetadataType}.
     *
     * @param response the response to deserialize
     * @return the metadata
     * @throws Exception if the metadata fails to deserialize
     */
    abstract MetadataType parseMetadata(HttpWebResponse response) throws Exception;
}
