package com.microsoft.aad.adal;

import java.io.UnsupportedEncodingException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.aad.adal.AuthenticationActivity.CustomWebViewClient;

class AuthenticationDialog {
    private Context mContext;
	private AuthenticationContext mAuthContext;
	private AuthenticationRequest mRequest;
	private  Handler mHandlerInView;
	
	public AuthenticationDialog(Handler handler,Context context, AuthenticationContext ctx,
			AuthenticationRequest request) {
		mContext = context;
		mAuthContext = ctx;
		mRequest = request;
	}
	
public void show(){

	mHandlerInView.post(new Runnable() {
        
        @Override
        public void run() {
        	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            View webviewInDialog = inflater.inflate(R.layout.dialog_authentication, null);
            
            final WebView webview = (WebView)webviewInDialog.findViewById(R.id.webView1);
            webview.getSettings().setJavaScriptEnabled(true);
            webview.requestFocus(View.FOCUS_DOWN);

            // Set focus to the view for touch event
            webview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    int action = event.getAction();
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
                        if (!view.hasFocus()) {
                            view.requestFocus();
                        }
                    }
                    return false;
                }
            });

            webview.getSettings().setLoadWithOverviewMode(true);
            webview.getSettings().setDomStorageEnabled(true);
            webview.getSettings().setUseWideViewPort(true);
            webview.getSettings().setBuiltInZoomControls(true);

            try {
                final String startUrl = helper.getStartUrl();

                final String stopRedirect = helper.getRedirectUrl();
                webview.setWebViewClient(new CustomWebViewClient(helper, stopRedirect, requestCode));
                webview.post(new Runnable() {
                    @Override
                    public void run() {
                        webview.loadUrl(startUrl);
                    }
                });

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            
            builder.setMessage("Example usage").setView(webviewInDialog).setCancelable(true);
            mDialog = builder.create();
            mDialog.show();                    
        }
	}
	
	class DialogWebViewClient extends BasicWebViewClient {
		private String mRedirect;

		private int mRequestCode;

		DialogWebViewClient(String redirect, int requestCode) {
			mRedirect = redirect;
			mRequestCode = requestCode;
		}

		public void showSpinner(boolean status){
			
		}

		public void sendResponse(int returnCode, Intent responseIntent){
			
		}

		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith(mRedirect)) {
				Intent resultIntent = new Intent();
				resultIntent
						.putExtra(
								AuthenticationConstants.Browser.RESPONSE_FINAL_URL,
								url);
				resultIntent.putExtra(
						AuthenticationConstants.Browser.RESPONSE_REQUEST_INFO,
						mRequest);
				mContext.onActivityResult(
						mRequestCode,
						AuthenticationConstants.UIResponse.BROWSER_CODE_COMPLETE,
						resultIntent);
				view.stopLoading();
				return true;
			}

			return false;
		}
	}
}
