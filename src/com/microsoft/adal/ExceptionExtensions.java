/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 */

package com.microsoft.adal;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * A helper for getting logging info out of an Exception
 */
final class ExceptionExtensions
{
    static String getExceptionMessage( Exception ex )
    {
        String message = ex.getMessage();
        
        if ( message == null )
        {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            message = sw.toString();                            
        }
        
        return message;
    }
}
