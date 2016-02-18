package com.microsoft.aad.adal;

import java.io.IOException;

/**
 * Created by imakhal on 2/17/2016.
 */
public class UnexpectedServerResponseException extends IOException {
    static final long serialVersionUID = 1;

    public UnexpectedServerResponseException(String detailMessage) {
        super(detailMessage);
    }
}
