// Copyright © Microsoft Open Technologies, Inc.
//
// All Rights Reserved
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
// OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
// ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
// PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
//
// See the Apache License, Version 2.0 for the specific language
// governing permissions and limitations under the License.

package com.microsoft.adal;

/**
 * response object
 * 
 * @author omercan
 * 
 */
class WebResponse {
	private HttpWebResponse response = null;
	private Exception responseException = null;
	private boolean processing = true;

	public HttpWebResponse getResponse() {
		return response;
	}

	public void setResponse(HttpWebResponse response) {
		this.response = response;
	}

	public Exception getResponseException() {
		return responseException;
	}

	public void setResponseException(Exception responseException) {
		this.responseException = responseException;
	}

	public boolean isProcessing() {
		return processing;
	}

	public void setProcessing(boolean processing) {
		this.processing = processing;
	}
}
