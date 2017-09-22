/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2012 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.amazonaws.auth.BasicAWSCredentials;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.AccountIdentifiers;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.Topology;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.exceptions.s3.PreconditionFailedException;
import com.eucalyptus.objectstorage.exceptions.s3.S3ErrorCodeStrings;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLocationType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType;
import com.eucalyptus.objectstorage.msgs.HeadBucketType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.objectstorage.providers.s3.S3ProviderClient;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.util.SynchronousClient;
import com.eucalyptus.util.SynchronousClient.SynchronousClientException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.walrus.WalrusBackend;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusDataRequestType;
import com.eucalyptus.walrus.msgs.WalrusDataResponseType;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.msgs.WalrusResponseType;
import com.eucalyptus.ws.EucalyptusRemoteFault;
import com.google.common.base.MoreObjects;

/**
 * The provider client that is used by the OSG to communicate with the Walrus backend. Because Walrus is IAM-aware, this provider does *not* perform
 * IAM policy checks itself.
 *
 * WalrusProviderClient leverages the AWS Java SDK for the GET/PUT data operations on Walrus. All metadata operations are handled using normal
 * Eucalyptus message delivery
 *
 */
@ObjectStorageProviders.ObjectStorageProviderClientProperty("walrus")
public class WalrusProviderClient extends S3ProviderClient {
  private static Logger LOG = Logger.getLogger(WalrusProviderClient.class);
  private static User osgUser = null;

  /**
   * Class for handling the message pass-thru
   *
   */
  protected static class WalrusClient extends SynchronousClient<WalrusRequestType, WalrusBackend> {
    WalrusClient() {
      super(osgUser.getUserId(), WalrusBackend.class);
    }

    public <REQ extends WalrusRequestType, RES extends WalrusResponseType> RES sendSyncA(final REQ request) throws Exception {
      request.setUser(osgUser);
      request.setUserId(osgUser.getUserId());
      return AsyncRequests.sendSync(configuration, request);
    }

    public <REQ extends WalrusDataRequestType, RES extends WalrusDataResponseType> RES sendSyncADataReq(final REQ request) throws Exception {
      request.setUser(osgUser);
      request.setUserId(osgUser.getUserId());
      return AsyncRequests.sendSync(configuration, request);
    }

  }

  @Override
  protected URI getUpstreamEndpoint() {
    return Topology.lookup(WalrusBackend.class).getUri();
  }

  @Override
  protected boolean doUsePathStyle() {
    return true;
  }

  @Override
  public void initialize() throws EucalyptusCloudException {
    super.initialize();
    try {
      osgUser = Accounts.lookupSystemAccountByAlias( AccountIdentifiers.OBJECT_STORAGE_WALRUS_ACCOUNT );
    } catch (AuthException e) {
      LOG.error("Failed to lookup system admin account. Cannot initialize Walrus provider client.", e);
      throw new EucalyptusCloudException(e);
    }
  }

  @Override
  public void check() throws EucalyptusCloudException {
    if (!Topology.isEnabled(WalrusBackend.class)) {
      throw new EucalyptusCloudException("No ENABLED WalrusBackend found. Cannot initialize fully.");
    }
    super.check();
  }

  /**
   * Simply looks up the currently enabled Walrus service.
   *
   * @return
   */
  protected WalrusClient getEnabledWalrusClient() throws ObjectStorageException {
    try {
      WalrusClient c = new WalrusClient();
      c.init();
      return c;
    } catch (SynchronousClientException e) {
      LOG.error("Error initializing client for walrus pass-thru", e);
      throw new ObjectStorageException("Could not initialize walrus client");
    }
  }

  /**
   * Walrus provider mapping maps all requests to the single ObjectStorage account used for interaction with Walrus
   */
  @Override
  protected BasicAWSCredentials getCredentials( ) throws AuthException, IllegalArgumentException {
    List<AccessKey> eucaAdminKeys = osgUser.getKeys();
    if (eucaAdminKeys != null && eucaAdminKeys.size() > 0) {
      return new BasicAWSCredentials(eucaAdminKeys.get(0).getAccessKey(), eucaAdminKeys.get(0).getSecretKey());
    } else {
      LOG.error(
          "No key found for osg user " + osgUser.getUserId() + " . Cannot map credentials for call to WalrusBackend backend for data operation");
      throw new AuthException("No access key found for backend call to WalrusBackend for UserId: " + osgUser.getUserId());
    }
  }

  /**
   * Do the request proxying
   *
   * @param request
   * @param walrusRequestClass
   * @param walrusResponseClass
   * @return
   */
  private <ObjResp extends ObjectStorageResponseType, ObjReq extends ObjectStorageRequestType, WalResp extends WalrusResponseType, WalReq extends WalrusRequestType> ObjResp proxyRequest(
      ObjReq request, Class<WalReq> walrusRequestClass, Class<WalResp> walrusResponseClass) throws EucalyptusCloudException {
    ObjectStorageException osge = null;
    try {
      WalrusClient c = getEnabledWalrusClient();
      WalReq walrusRequest = MessageMapper.INSTANCE.proxyWalrusRequest(walrusRequestClass, request);
      WalResp walrusResponse = c.sendSyncA(walrusRequest);
      ObjResp reply = MessageMapper.INSTANCE.proxyWalrusResponse(request, walrusResponse);
      return reply;
    } catch (ServiceDispatchException e) {
      Throwable rootCause = e.getRootCause();
      if (rootCause != null) {
        if (rootCause instanceof WalrusException) {
          osge = MessageMapper.INSTANCE.proxyWalrusException((WalrusException) rootCause);
        } else {
          throw new EucalyptusCloudException(rootCause);
        }
      }
    } catch (Exception e) {
      final EucalyptusRemoteFault remoteFault = Exceptions.findCause(e, EucalyptusRemoteFault.class);
      if (remoteFault != null) {
        handleRemoteFault(remoteFault);
      } else {
        throw new EucalyptusCloudException(e);
      }
    }
    if (osge != null) {
      throw osge;
    }
    throw new EucalyptusCloudException("Unable to obtain reply from dispatcher.");
  }

  private static S3Exception mapWalrusExceptionToS3Exception(EucalyptusCloudException ex) {
    if (ex instanceof S3Exception) {
      return (S3Exception) ex;
    } else {
      LOG.debug("Mapping exception from Walrus to 500-InternalError", ex);
      InternalErrorException s3Ex = new InternalErrorException();
      s3Ex.initCause(ex);
      return s3Ex;
    }
  }

  private <ObjResp extends ObjectStorageDataResponseType, ObjReq extends ObjectStorageDataRequestType, WalResp extends WalrusDataResponseType, WalReq extends WalrusDataRequestType> ObjResp proxyDataRequest(
      ObjReq request, Class<WalReq> walrusRequestClass, Class<WalResp> walrusResponseClass) throws EucalyptusCloudException {
    ObjectStorageException osge = null;
    try {
      WalrusClient c = getEnabledWalrusClient();
      WalReq walrusRequest = MessageMapper.INSTANCE.proxyWalrusDataRequest(walrusRequestClass, request);
      WalResp walrusResponse = c.sendSyncADataReq(walrusRequest);
      ObjResp reply = MessageMapper.INSTANCE.proxyWalrusDataResponse(request, walrusResponse);
      return reply;
    } catch (ServiceDispatchException e) {
      Throwable rootCause = e.getRootCause();
      if (rootCause != null) {
        if (rootCause instanceof WalrusException) {
          osge = MessageMapper.INSTANCE.proxyWalrusException((WalrusException) rootCause);
        } else {
          throw new EucalyptusCloudException(rootCause);
        }
      }
    } catch (Exception e) {
      final EucalyptusRemoteFault remoteFault = Exceptions.findCause(e, EucalyptusRemoteFault.class);
      if (remoteFault != null) {
        handleRemoteFault(remoteFault);
      } else {
        throw new EucalyptusCloudException(e);
      }
    }
    if (osge != null) {
      throw osge;
    }
    throw new EucalyptusCloudException("Unable to obtain reply from dispatcher.");
  }

  protected static void handleRemoteFault(EucalyptusRemoteFault remoteFault) throws S3Exception {
    final HttpResponseStatus status = HttpResponseStatus.valueOf( MoreObjects.firstNonNull(remoteFault.getStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR.getCode( ) ) );
    final String code = remoteFault.getFaultCode();
    final String message = remoteFault.getFaultDetail();
    final String exceptionType = remoteFault.getFaultString();

    if (HttpResponseStatus.NOT_ACCEPTABLE.equals(status)) {
      // Use the message
      if (exceptionType.contains(S3ErrorCodeStrings.BucketNotEmpty)) {
        throw new BucketNotEmptyException();
      } else if (exceptionType.contains(S3ErrorCodeStrings.NoSuchBucket)) {
        throw new NoSuchBucketException();
      } else if (exceptionType.contains(S3ErrorCodeStrings.NoSuchKey)) {
        throw new NoSuchKeyException();
      } else if (exceptionType.contains(S3ErrorCodeStrings.AccessDenied)) {
        throw new AccessDeniedException(null, message);
      } else {
        throw new InternalErrorException(null, message);
      }
    } else if (HttpResponseStatus.CONFLICT.equals(status)) {
      throw new BucketNotEmptyException();
    } else if (HttpResponseStatus.PRECONDITION_FAILED.equals(status)) {
      throw new PreconditionFailedException(null, message);
    } else if (HttpResponseStatus.FORBIDDEN.equals(status)) {
      throw new AccessDeniedException(null, message);
    } else if (HttpResponseStatus.NOT_FOUND.equals(status)) {
      if (exceptionType.contains("Bucket")) {
        throw new NoSuchBucketException();
      } else if (exceptionType.contains("Key")) {
        throw new NoSuchKeyException();
      }
    }
    throw new S3Exception(code, message, status);
  }

  /*
   * ------------------------- Service Operations -------------------------
   */

  @Override
  public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.ListAllMyBucketsType.class,
          com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  /*
   * ------------------------- Bucket Operations -------------------------
   */
  @Override
  public CreateBucketResponseType createBucket(CreateBucketType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.CreateBucketType.class, com.eucalyptus.walrus.msgs.CreateBucketResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.DeleteBucketType.class, com.eucalyptus.walrus.msgs.DeleteBucketResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public HeadBucketResponseType headBucket(HeadBucketType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.HeadBucketType.class, com.eucalyptus.walrus.msgs.HeadBucketResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyType.class,
          com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public ListBucketResponseType listBucket(ListBucketType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.ListBucketType.class, com.eucalyptus.walrus.msgs.ListBucketResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public SetBucketLoggingStatusResponseType setBucketLoggingStatus(SetBucketLoggingStatusType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public GetBucketLoggingStatusResponseType getBucketLoggingStatus(GetBucketLoggingStatusType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public SetBucketVersioningStatusResponseType setBucketVersioningStatus(SetBucketVersioningStatusType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public ListVersionsResponseType listVersions(ListVersionsType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public DeleteVersionResponseType deleteVersion(DeleteVersionType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  /*
   * ------------------------- Object Operations -------------------------
   */

  // GET and PUT object are the same as the base S3ProviderClient.

  @Override
  public PostObjectResponseType postObject(PostObjectType request) throws S3Exception {
    throw new NotImplementedException("PostObject");
  }

  @Override
  public CopyObjectResponseType copyObject(CopyObjectType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.CopyObjectType.class, com.eucalyptus.walrus.msgs.CopyObjectResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws S3Exception {
    try {
      DeleteObjectResponseType response =
          proxyRequest(request, com.eucalyptus.walrus.msgs.DeleteObjectType.class, com.eucalyptus.walrus.msgs.DeleteObjectResponseType.class);
      // HACK: Remote Walrus cannot send HTTP status over wire. Setting the status here for now - EUCA-9425
      if (response.getStatus() == null && response.getStatusMessage().equals("NO CONTENT")) {
        response.setStatus(HttpResponseStatus.NO_CONTENT);
      }
      return response;
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws S3Exception {
    try {
      return proxyRequest(request, com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyType.class,
          com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws S3Exception {
    throw new InternalErrorException(null, "Operation not supported by walrusbackend");
  }

  @Override
  public InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws S3Exception {
    try {
      return proxyDataRequest(request, com.eucalyptus.walrus.msgs.InitiateMultipartUploadType.class,
          com.eucalyptus.walrus.msgs.InitiateMultipartUploadResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public CompleteMultipartUploadResponseType completeMultipartUpload(CompleteMultipartUploadType request) throws S3Exception {
    try {
      return proxyDataRequest(request, com.eucalyptus.walrus.msgs.CompleteMultipartUploadType.class,
          com.eucalyptus.walrus.msgs.CompleteMultipartUploadResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }

  @Override
  public AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) throws S3Exception {
    try {
      return proxyDataRequest(request, com.eucalyptus.walrus.msgs.AbortMultipartUploadType.class,
          com.eucalyptus.walrus.msgs.AbortMultipartUploadResponseType.class);
    } catch (EucalyptusCloudException e) {
      LOG.debug("Error response from WalrusBackend", e);
      throw mapWalrusExceptionToS3Exception(e);
    }
  }
}
