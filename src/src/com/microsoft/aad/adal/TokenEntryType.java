// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.aad.adal;

/**
 * Internal class representing the entry type for stored {@link TokenCacheItem}
 */
enum TokenEntryType {
    /**
     * Represents the regular token entry. 
     * {@link TokenCacheItem} stored for regular token entry will have resource, 
     * access token, client id store. 
     * If it's also a MRRT item, MRRT flag will be marked as true. 
     * If it's also a FRT item, FoCI field will be populated with the family client Id 
     * server returned. 
     */
    REGULAR_TOKEN_ENTRY, 
    
    /**
     * Represents the MRRT token entry. 
     * {@link TokenCacheItem} stored for MRRT token entry will not have resource 
     * and access token store. 
     * MRRT flag will be set as true. 
     * If it's also a FRT item, FoCI field will be populated with the family client Id 
     * server returned. 
     */
    MRRT_TOKEN_ENTRY, 
    
    /**
     * Represents the FRT token entry. 
     * {@link TokenCacheItem} stored for FRT token entry will not have resource, access token
     * and client id stored. FoCI field be will populated with the value server returned. 
     */
    FRT_TOKEN_ENTRY
}
