package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data container for WebFinger responses.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033">RFC-7033</a>
 */
class WebFingerMetadata {

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.1">RFC-7033</a>
     */
    @SerializedName("subject")
    private String mSubject;

    /**
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">RFC-7033</a>
     */
    @SerializedName("links")
    private List<Link> mLinks;

    /**
     * Gets the subject.
     *
     * @return the subject
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Sets the subject.
     *
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.mSubject = subject;
    }

    /**
     * Gets the links.
     *
     * @return the links
     */
    public List<Link> getLinks() {
        return mLinks;
    }

    /**
     * Sets the links.
     *
     * @param links the links to set
     */
    public void setLinks(List<Link> links) {
        this.mLinks = links;
    }
}
