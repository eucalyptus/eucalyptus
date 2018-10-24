/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the
 *   distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus;

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.BadDigestException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyOwnedByYouException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException;
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
import com.eucalyptus.objectstorage.exceptions.s3.MethodNotAllowedException;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusDataRequestType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.msgs.WalrusResponseType;

/**
 * Provides message mapping functions for ObjectStorage types <-> Walrus types
 *
 * @author zhill
 *
 */
public enum MessageMapper {
  INSTANCE;

  private static final Logger LOG = Logger.getLogger(MessageMapper.class);

  public <O extends WalrusDataRequestType, I extends ObjectStorageDataRequestType> O proxyWalrusDataRequest(Class<O> outputClass, I request) {
    O outputRequest = (O) Classes.newInstance(outputClass);
    outputRequest = (O) (MessageProxy.mapExcludeNulls(request, outputRequest));
    outputRequest.regardingUserRequest(request);
    return outputRequest;
  }

  /**
   * Maps the OSG request type to the Walrus type, including BaseMessage handling for 'regarding' and correlationId mapping
   *
   * @param outputClass
   * @param request
   * @return
   */
  public <O extends WalrusRequestType, I extends ObjectStorageRequestType> O proxyWalrusRequest(Class<O> outputClass, I request) {
    O outputRequest = (O) Classes.newInstance(outputClass);
    outputRequest = (O) (MessageProxy.mapExcludeNulls(request, outputRequest));
    outputRequest.regardingUserRequest(request);
    return outputRequest;
  }

  public <O extends ObjectStorageDataResponseType, T extends ObjectStorageDataRequestType, I extends WalrusDataResponseType> O proxyWalrusDataResponse(
      T initialRequest, I response) {
    O outputResponse = (O) initialRequest.getReply();
    outputResponse = (O) (MessageProxy.mapExcludeNulls(response, outputResponse));
    return outputResponse;
  }

  /**
   * Maps the response from walrus to the appropriate response type for OSG
   *
   * @param initialRequest
   * @param response
   * @return
   */
  public <O extends ObjectStorageResponseType, T extends ObjectStorageRequestType, I extends WalrusResponseType> O proxyWalrusResponse(
      T initialRequest, I response) {
    O outputResponse = (O) initialRequest.getReply();
    outputResponse = (O) (MessageProxy.mapExcludeNulls(response, outputResponse));
    return outputResponse;
  }

  public <O extends S3Exception, T extends WalrusException> O proxyWalrusException(T initialException) throws EucalyptusCloudException {
    try {
      Class c = exceptionMap.get(initialException.getClass());
      if (c == null) {
        LOG.warn("an attempt to proxy a walrus exception failed because there is no mapping for " + initialException.getClass().getName());
        WalrusException proxied = new WalrusException("no mapping for " + initialException.getClass().getName(), initialException);
        return proxyWalrusException(proxied);
      }
      O outputException = (O) c.newInstance();
      outputException = (O) (WalrusExceptionProxy.mapExcludeNulls(initialException, outputException));
      return outputException;
    } catch (Exception e) {
      throw new EucalyptusCloudException(e);
    }
  }

  private static HashMap<Class<? extends WalrusException>, Class<? extends S3Exception>> exceptionMap =
      new HashMap<Class<? extends WalrusException>, Class<? extends S3Exception>>();

  static {
    // Populate the map
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
    exceptionMap.put(com.eucalyptus.walrus.exceptions.InternalErrorException.class, InternalErrorException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.InvalidTargetBucketForLoggingException.class, InvalidTargetBucketForLoggingException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.NoSuchBucketException.class, NoSuchBucketException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.NoSuchEntityException.class, NoSuchKeyException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.NotAuthorizedException.class, AccessDeniedException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.NotImplementedException.class, NotImplementedException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.NotReadyException.class, ServiceUnavailableException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.PreconditionFailedException.class, PreconditionFailedException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.TooManyBucketsException.class, TooManyBucketsException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.WalrusException.class, InternalErrorException.class);
    exceptionMap.put(com.eucalyptus.walrus.exceptions.MethodNotAllowedException.class, MethodNotAllowedException.class);
  }
}
