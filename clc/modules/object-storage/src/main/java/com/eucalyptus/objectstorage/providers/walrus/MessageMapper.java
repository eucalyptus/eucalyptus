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

import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.BadDigestException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyOwnedByYouException;
import com.eucalyptus.objectstorage.exceptions.s3.EntityTooLargeException;
import com.eucalyptus.objectstorage.exceptions.s3.InlineDataTooLargeException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidArgumentException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidBucketNameException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidTargetBucketForLoggingException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.exceptions.s3.PreconditionFailedException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.exceptions.s3.ServiceUnavailableException;
import com.eucalyptus.objectstorage.exceptions.s3.TooManyBucketsException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.msgs.WalrusDataRequestType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.msgs.WalrusResponseType;
import com.eucalyptus.walrus.exceptions.WalrusException;
import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Provides message mapping functions for ObjectStorage types <-> Walrus types
 * @author zhill
 *
 */
public enum MessageMapper {
	INSTANCE;

    private static final Logger LOG = Logger.getLogger(MessageMapper.class);

    public <O extends WalrusDataRequestType, I extends ObjectStorageDataRequestType>  O proxyWalrusDataRequest(Class<O> outputClass, I request) {
        O outputRequest = (O) Classes.newInstance(outputClass);
        outputRequest = (O)(MessageProxy.mapExcludeNulls(request, outputRequest));
        outputRequest.regardingUserRequest(request);
        return outputRequest;
    }

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

    public <O extends ObjectStorageDataResponseType, T extends ObjectStorageDataRequestType, I extends WalrusDataResponseType>  O proxyWalrusDataResponse(T initialRequest, I response) {
        O outputResponse = (O)initialRequest.getReply();
        outputResponse = (O)(MessageProxy.mapExcludeNulls(response, outputResponse));
        return outputResponse;
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

	public <O extends S3Exception, T extends WalrusException>  O proxyWalrusException(T initialException) throws EucalyptusCloudException {
		try {
			Class c = exceptionMap.get(initialException.getClass());
            if (c == null) {
                LOG.warn("an attempt to proxy a walrus exception failed because there is no mapping for " + initialException.getClass().getName());
                WalrusException proxied = new WalrusException("no mapping for " + initialException.getClass().getName(), initialException);
                return proxyWalrusException(proxied);
            }
			O outputException = (O) c.newInstance();
			outputException = (O)(WalrusExceptionProxy.mapExcludeNulls(initialException, outputException));
			return outputException;
		} catch (Exception e) {
			throw new EucalyptusCloudException(e);
		}
	}

    private static HashMap<Class<? extends WalrusException>, Class<? extends S3Exception>> exceptionMap = new HashMap<Class<? extends WalrusException>, Class<? extends S3Exception>>();

    static {
        //Populate the map
        exceptionMap.put(com.eucalyptus.walrus.exceptions.AccessDeniedException.class, AccessDeniedException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.BucketAlreadyExistsException.class, BucketAlreadyExistsException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.BucketAlreadyOwnedByYouException.class, BucketAlreadyOwnedByYouException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.BucketNotEmptyException.class, BucketNotEmptyException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.ContentMismatchException.class, BadDigestException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.DecryptionFailedException.class, InternalErrorException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.EntityTooLargeException.class, EntityTooLargeException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.HeadAccessDeniedException.class, AccessDeniedException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.HeadNoSuchBucketException.class, NoSuchBucketException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.HeadNoSuchEntityException.class, NoSuchKeyException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.InlineDataTooLargeException.class, InlineDataTooLargeException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.InvalidArgumentException.class, InvalidArgumentException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.InvalidBucketNameException.class, InvalidBucketNameException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.InvalidTargetBucketForLoggingException.class, InvalidTargetBucketForLoggingException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.NoSuchBucketException.class, NoSuchBucketException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.NoSuchEntityException.class, NoSuchKeyException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.NotAuthorizedException.class, AccessDeniedException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.NotImplementedException.class, NotImplementedException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.NotReadyException.class, ServiceUnavailableException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.PreconditionFailedException.class, PreconditionFailedException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.TooManyBucketsException.class, TooManyBucketsException.class);
        exceptionMap.put(com.eucalyptus.walrus.exceptions.WalrusException.class, InternalErrorException.class);
    }
}
