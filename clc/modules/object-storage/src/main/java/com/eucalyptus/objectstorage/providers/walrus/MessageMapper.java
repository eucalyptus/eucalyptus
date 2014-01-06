/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus;

import com.eucalyptus.objectstorage.MessageProxy;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Strings;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.msgs.WalrusResponseType;
import com.eucalyptus.walrus.msgs.WalrusDataRequestType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import com.eucalyptus.walrus.exceptions.WalrusException;

/**
 * Provides message mapping functions for ObjectStorage types <-> Walrus types
 * @author zhill
 *
 */
public enum MessageMapper {
	INSTANCE;

	/**
	 * Maps the OSG request type to the Walrus type, including BaseMessage handling for 'regarding' and correlationId mapping
	 * @param outputClass
	 * @param request
	 * @return
	 */
	public <O extends WalrusRequestType, I extends ObjectStorageRequestType>  O proxyWalrusRequest(Class<O> outputClass, I request) {
		O outputRequest = (O) Classes.newInstance(outputClass);
		outputRequest = (O)(MessageProxy.mapExcludeNulls(request, outputRequest));
		outputRequest.regardingUserRequest(request);
		return outputRequest;
	}
	
	/**
	 * Maps the response from walrus to the appropriate response type for OSG
	 * @param initialRequest
	 * @param response
	 * @return
	 */
	public <O extends ObjectStorageResponseType, T extends ObjectStorageRequestType, I extends WalrusResponseType>  O proxyWalrusResponse(T initialRequest, I response) {
		O outputResponse = (O)initialRequest.getReply();
		outputResponse = (O)(MessageProxy.mapExcludeNulls(response, outputResponse));
		return outputResponse;
	}

	public <O extends ObjectStorageException, T extends WalrusException>  O proxyWalrusException(T initialException) throws EucalyptusCloudException {
		String cname = initialException.getClass().getName().replaceAll("com\\.eucalyptus\\.walrus\\.exceptions", "com\\.eucalyptus\\.objectstorage\\.exceptions");
		try {
			Class c = Class.forName(cname);
			O outputException = (O) c.newInstance();
			outputException = (O)(WalrusExceptionProxy.mapExcludeNulls(initialException, outputException));
			return outputException;
		} catch (Exception e) {
			throw new EucalyptusCloudException(e);
		}
	}

}
