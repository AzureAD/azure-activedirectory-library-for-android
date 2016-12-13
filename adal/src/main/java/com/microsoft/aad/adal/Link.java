package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

/**
 * Data container for Link elements in {@link WebFingerMetadata}.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
 */
class Link {

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
    public String getRel() {
        return mRel;
    }

    /**
     * Sets the rel.
     *
     * @param rel the rel to set
     */
    public void setRel(String rel) {
        this.mRel = rel;
    }

    /**
     * Gets the href.
     *
     * @return the href
     */
    public String getHref() {
        return mHref;
    }

    /**
     * Sets the href.
     *
     * @param href the href to set
     */
    public void setHref(String href) {
        this.mHref = href;
    }
}
