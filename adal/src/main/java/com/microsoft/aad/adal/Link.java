package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for Link elements in {@link WebFingerMetadata}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
 */
final class Link {

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("rel")
    private String mRel;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("href")
    private String mHref;

    /**
     * Gets the rel.
     *
     * @return the rel
     */
    String getRel() {
        return mRel;
    }

    /**
     * Sets the rel.
     *
     * @param rel the rel to set
     */
    void setRel(final String rel) {
        this.mRel = rel;
    }

    /**
     * Gets the href.
     *
     * @return the href
     */
    String getHref() {
        return mHref;
    }

    /**
     * Sets the href.
     *
     * @param href the href to set
     */
    void setHref(final String href) {
        this.mHref = href;
    }
}
