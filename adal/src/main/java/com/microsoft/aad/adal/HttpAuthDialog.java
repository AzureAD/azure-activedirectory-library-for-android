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

package com.microsoft.aad.adal;

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

import com.microsoft.aad.adal.R;

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

    private void createDialog() {
        LayoutInflater factory = LayoutInflater.from(mContext);
        View v = factory.inflate(R.layout.http_auth_dialog, null);
        mUsernameView = (EditText)v.findViewById(R.id.editUserName);
        mPasswordView = (EditText)v.findViewById(R.id.editPassword);
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
                                if (mCancelListener != null)
                                    mCancelListener.onCancel();
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (mCancelListener != null)
                            mCancelListener.onCancel();
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
