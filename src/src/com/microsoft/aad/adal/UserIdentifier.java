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

import java.io.Serializable;

import android.content.Intent;
import android.text.TextUtils;

public class UserIdentifier implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5739521222033787270L;

	public enum UserIdentifierType {
        UniqueId, OptionalDisplayableId, RequiredDisplayableId
    }

    private String mId;

    private UserIdentifierType mType;

    private static final String AnyUserId = "AnyUser";

    private static final UserIdentifier AnyUser = new UserIdentifier(AnyUserId,
            UserIdentifierType.UniqueId);

    public UserIdentifier(String id, UserIdentifierType userType) {
        this.mId = id;
        this.mType = userType;
    }

    public String getId() {
        return mId;
    }

    public UserIdentifierType getType() {
        return mType;
    }

    public static UserIdentifier getAnyUser() {
        return AnyUser;
    }

    boolean anyUser() {
        if(mType == null || mId == null){
            return false;
        }
            
        return mType.equals(AnyUser.mType) && mId.equalsIgnoreCase(AnyUserId);
    }

    String getUniqueId() {
        return (!this.anyUser() && this.mType.equals(UserIdentifierType.UniqueId)) ? this.mId : "";
    }

    String getDisplayableId() {
        return (!this.anyUser() && (this.mType.equals(UserIdentifierType.RequiredDisplayableId) || this.mType
                .equals(UserIdentifierType.OptionalDisplayableId))) ? this.mId : "";
    }

    public static UserIdentifier createFromIntent(Intent callingIntent) {

        UserIdentifier userid = new UserIdentifier("", UserIdentifierType.OptionalDisplayableId);

        if (callingIntent != null && callingIntent.hasExtra(AuthenticationConstants.Broker.ACCOUNT_USERID_TYPE)) {
            String idType = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERID_TYPE);

            userid.mId = callingIntent
                    .getStringExtra(AuthenticationConstants.Broker.ACCOUNT_USERID_ID);
            if(!TextUtils.isEmpty(idType)){
                userid.mType = UserIdentifierType.valueOf(idType);
            }
        }

        return userid;
    }
}
