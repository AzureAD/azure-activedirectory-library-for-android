package com.microsoft.adal;

 

/**
 * Represents an error condition
 */
public class AzureException extends RuntimeException {
    static final long serialVersionUID = 1;

    /**
     * Constructs a new AzureException.
     */
    public AzureException() {
        super();
    }

    /**
     * Constructs a new AzureException.
     * 
     * @param message
     *            the detail message of this exception
     */
    public AzureException(String message) {
        super(message);
    }

    /**
     * Constructs a new AzureException.
     * 
     * @param message
     *            the detail message of this exception
     * @param throwable
     *            the cause of this exception
     */
    public AzureException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructs a new AzureException.
     * 
     * @param throwable
     *            the cause of this exception
     */
    public AzureException(Throwable throwable) {
        super(throwable);
    }
}
