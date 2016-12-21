package com.microsoft.aad.adal;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data container for WebFinger responses.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033">RFC-7033</a>
 */
final class WebFingerMetadata {

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
    String getSubject() {
        return mSubject;
    }

    /**
     * Sets the subject.
     *
     * @param subject the subject to set
     */
    void setSubject(final String subject) {
        this.mSubject = subject;
    }

    /**
     * Gets the links.
     *
     * @return the links
     */
    List<Link> getLinks() {
        return mLinks;
    }

    /**
     * Sets the links.
     *
     * @param links the links to set
     */
    void setLinks(final List<Link> links) {
        this.mLinks = links;
    }
}
