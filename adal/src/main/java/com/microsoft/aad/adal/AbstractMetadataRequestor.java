package com.microsoft.aad.adal;

import com.google.gson.Gson;

import java.util.UUID;

/**
 * Creates correlatable Requests to HTTP accessible resources.
 */
abstract class AbstractMetadataRequestor<MetadataType, MetadataRequestOptions>
        implements Correlatable {

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

    @Override
    public final void setCorrelationId(final UUID requestCorrelationId) {
        mCorrelationId = requestCorrelationId;
    }

    @Override
    public final UUID getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Gets the thread-safe lazy-initialized parser for JSON.
     *
     * @return the gson instance
     */
    protected synchronized Gson parser() {
        if (null == mGson) {
            mGson = new Gson();
        }

        return mGson;
    }

    /**
     * Gets the IWebRequestHandler used to make requests.
     *
     * @return the IWebRequestHandler
     */
    protected IWebRequestHandler getWebrequestHandler() {
        return mWebrequestHandler;
    }

    abstract MetadataType requestMetadata(MetadataRequestOptions options) throws Exception;
}
