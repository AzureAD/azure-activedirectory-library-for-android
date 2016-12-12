package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data container for WebFinger responses
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033">RFC-7033</a>
 */
class WebFingerMetadata {

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.1">RFC-7033</a>
     */
    @SerializedName("subject")
    String mSubject;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("links")
    List<Link> mLinks;
}
