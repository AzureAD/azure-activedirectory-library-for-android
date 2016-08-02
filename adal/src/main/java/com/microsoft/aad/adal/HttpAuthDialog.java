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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

/**
 * Http auth dialog for ntlm challenge on webview.
 */
class HttpAuthDialog {

    private final Context mContext;

    private final String mHost;

    private final String mRealm;

    private AlertDialog mDialog;

    private EditText mUsernameView;

    private EditText mPasswordView;

    private OkListener mOkListener;

    private CancelListener mCancelListener;

    /**
     * Creates Credential dialog for http auth.
     */
    public HttpAuthDialog(Context context, String host, String realm) {
        mContext = context;
        mHost = host;
        mRealm = realm;
        mDialog = null;
        createDialog();
    }

    public void setOkListener(OkListener okListener) {
        mOkListener = okListener;
    }

    public void setCancelListener(CancelListener cancelListener) {
        mCancelListener = cancelListener;
    }

    public void show() {
        mDialog.show();
        mUsernameView.requestFocus();
    }

    @SuppressLint("InflateParams")
    private void createDialog() {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View v = factory.inflate(R.layout.http_auth_dialog, null);
        mUsernameView = (EditText) v.findViewById(R.id.editUserName);
        mPasswordView = (EditText) v.findViewById(R.id.editPassword);
        mPasswordView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                    return true;
                }
                return false;
            }
        });

        String title = mContext.getText(R.string.http_auth_dialog_title).toString();

        mDialog = new AlertDialog.Builder(mContext)
                .setTitle(title)
                .setView(v)
                .setPositiveButton(R.string.http_auth_dialog_login,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mOkListener != null) {
                                    mOkListener.onOk(mHost, mRealm, mUsernameView.getText()
                                            .toString(), mPasswordView.getText().toString());
                                }
                            }
                        })
                .setNegativeButton(R.string.http_auth_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (mCancelListener != null) {
                                    mCancelListener.onCancel();
                                }
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (mCancelListener != null) {
                            mCancelListener.onCancel();
                        }
                    }
                }).create();
    }

    public interface OkListener {
        void onOk(String host, String realm, String username, String password);
    }

    public interface CancelListener {
        void onCancel();
    }
}
