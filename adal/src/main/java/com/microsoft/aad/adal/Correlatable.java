package com.microsoft.aad.adal;

import java.util.UUID;

/**
 * Specifies an Object or Action that can be correlated.
 */
interface Correlatable {

    /**
     * Sets the correlation id.
     *
     * @param correlationId the id
     */
    void setCorrelationId(UUID correlationId);

    /**
     * Gets the correlation id.
     *
     * @return the id
     */
    UUID getCorrelationId();
}
