package com.microsoft.adal;

/**
 * response object
 * 
 * @author omercan
 * 
 */
public class WebResponse {
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
