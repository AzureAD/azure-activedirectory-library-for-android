package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for Link elements in {@link WebFingerMetadata}
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
 */
class Link {

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("rel")
    String mRel;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("href")
    String mHref;
}
