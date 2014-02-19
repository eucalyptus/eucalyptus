/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.objectstorage.providers.walrus;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.context.ServiceDispatchException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.HeadObjectType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.AccessKey;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.exceptions.ObjectStorageException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
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
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.providers.s3.S3ProviderClient;
import com.eucalyptus.objectstorage.providers.s3.S3ProviderConfiguration;
import com.eucalyptus.objectstorage.util.S3Client;
import com.eucalyptus.util.EucalyptusCloudException;
import com.eucalyptus.util.SynchronousClient;
import com.eucalyptus.util.SynchronousClient.SynchronousClientException;
import com.eucalyptus.util.async.AsyncRequests;
import com.eucalyptus.walrus.Walrus;
import com.eucalyptus.walrus.exceptions.WalrusException;
import com.eucalyptus.walrus.msgs.WalrusRequestType;
import com.eucalyptus.walrus.msgs.WalrusResponseType;

import java.util.List;

/**
 * The provider client that is used by the OSG to communicate with the Walrus backend.
 * Because Walrus is IAM-aware, this provider does *not* perform IAM policy checks itself.
 * 
 * WalrusProviderClient leverages the AWS Java SDK for the GET/PUT data operations on Walrus.
 * All metadata operations are handled using normal Eucalyptus message delivery
 *
 */
@ObjectStorageProviders.ObjectStorageProviderClientProperty("walrus")
public class WalrusProviderClient extends S3ProviderClient {
	private static Logger LOG = Logger.getLogger(WalrusProviderClient.class);
	private static User systemAdmin = null;

    /**
	 * Class for handling the message pass-thru
	 *
	 */
	private static class WalrusClient extends SynchronousClient<WalrusRequestType, Walrus> {
        private User walrusUser;

		WalrusClient( final String userId ) {
			// super( userId, Walrus.class );
			super( systemAdmin.getUserId(), Walrus.class );
		}

	        public <REQ extends WalrusRequestType,RES extends WalrusResponseType> RES sendSyncA( final REQ request) throws Exception {
    			// request.setEffectiveUserId( userId );
	        	request.setUser( systemAdmin );
    			return AsyncRequests.sendSync( configuration, request );
  		}

	}

	@Override
	protected AmazonS3Client getS3Client(User requestUser, String requestAWSAccessKeyId) throws InternalErrorException {
		//TODO: this should be enhanced to share clients/use a pool for efficiency.
		if (s3Client == null) {
			synchronized(this) {
				boolean useHttps = false;
				if(S3ProviderConfiguration.getS3UseHttps() != null && S3ProviderConfiguration.getS3UseHttps()) {
					useHttps = true;
				}
				AWSCredentials credentials = null;
				try {
					credentials = mapCredentials(requestUser, requestAWSAccessKeyId);
				} catch(Exception e) {
                    LOG.error("Error mapping credentials for user " + (requestUser != null ? requestUser.getUserId() : "null") + " for walrus backend call.", e);
                    InternalErrorException ex = new InternalErrorException("Cannot construct s3client due to inability to map credentials for user: " +  (requestUser != null ? requestUser.getUserId() : "null"));
                    ex.initCause(e);
                    throw ex;
				}
				
				s3Client = new S3Client(credentials, useHttps);
				s3Client.setS3Endpoint(Topology.lookup(Walrus.class).getUri().toString());
				s3Client.setUsePathStyle(!S3ProviderConfiguration.getS3UseBackendDns());
			}
		}
		return s3Client.getS3Client();
	}


	@Override
	public void initialize() throws EucalyptusCloudException {
		try {
			systemAdmin = Accounts.lookupSystemAdmin();
		} catch (AuthException e) {
			LOG.warn("Failed to lookup system admin account");
			throw new EucalyptusCloudException(e);
		}
	}

	@Override
	public void check() throws EucalyptusCloudException { }
	
	/**
	 * Simply looks up the currently enabled Walrus service.
	 * @return
	 */
	private WalrusClient getEnabledWalrusClient() throws ObjectStorageException {
		try {
            WalrusClient c = new WalrusClient(null);
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
	protected  BasicAWSCredentials mapCredentials(User requestUser, String requestAWSAccessKeyId) throws AuthException, IllegalArgumentException {
		/*
		if(requestUser == null && requestAWSAccessKeyId == null) {
			throw new IllegalArgumentException("Null user and access key");
		}
		
		AccessKey userKey = null;
		try {
			userKey = requestUser.getKey(requestAWSAccessKeyId);
		} catch(AuthException e){
			LOG.trace("No valid access key for user " + requestUser.getUserId() + " found. Checking against system user");
			userKey = null;
		}
		
		if(userKey == null) {
			if(Principals.systemUser().equals(requestUser)) {
				//Map this to the Eucalyptus/Admin account.
				try {
					List<AccessKey> eucaAdminKeys = Accounts.lookupSystemAdmin().getKeys();
					if(eucaAdminKeys != null && eucaAdminKeys.size() > 0) {
						userKey = eucaAdminKeys.get(0);
					} else {
						userKey = null;
					}
				} catch(AuthException e) {
					LOG.trace("Exception looking up system admin keys for walrus credential mapping. User: " + requestUser.getUserId(), e);
					userKey = null;
				}
			}
			if(userKey == null) {
				LOG.error("No key found for user " + requestUser.getUserId() + " . Cannot map credentials for call to Walrus backend for data operation");
				throw new AuthException("No access key found for backend call to Walrus for UserId: " + requestUser.getUserId());
			}
		}
		return new BasicAWSCredentials(userKey.getAccessKey(), userKey.getSecretKey());
		*/
		
		List<AccessKey> eucaAdminKeys = systemAdmin.getKeys();
		if(eucaAdminKeys != null && eucaAdminKeys.size() > 0) {
			return new BasicAWSCredentials( eucaAdminKeys.get(0).getAccessKey(),  eucaAdminKeys.get(0).getSecretKey());
		} else {
			LOG.error("No key found for user " + requestUser.getUserId() + " . Cannot map credentials for call to Walrus backend for data operation");
			throw new AuthException("No access key found for backend call to Walrus for UserId: " + requestUser.getUserId());
		}
	}
	
	/**
	 * Do the request proxying
	 * @param request
	 * @param walrusRequestClass
	 * @param walrusResponseClass
	 * @return
	 */
	private <ObjResp extends ObjectStorageResponseType, 
	ObjReq extends ObjectStorageRequestType, 
	WalResp extends WalrusResponseType, 
	WalReq extends WalrusRequestType> ObjResp proxyRequest(ObjReq request, Class<WalReq> walrusRequestClass, Class<WalResp> walrusResponseClass) throws EucalyptusCloudException {
		ObjectStorageException osge = null;
		try  {
			WalrusClient c = getEnabledWalrusClient();
			WalReq walrusRequest = MessageMapper.INSTANCE.proxyWalrusRequest(walrusRequestClass, request);
			WalResp walrusResponse = c.sendSyncA(walrusRequest);
			ObjResp reply = MessageMapper.INSTANCE.proxyWalrusResponse(request, walrusResponse);
			return reply;
		} catch(ServiceDispatchException e) {
			Throwable rootCause = e.getRootCause();
			if(rootCause != null) {
				if (rootCause instanceof WalrusException) {
					osge = MessageMapper.INSTANCE.proxyWalrusException((WalrusException)rootCause);
				} else {
					throw new EucalyptusCloudException(rootCause);
				}
			}
		} catch(Exception e) {
			throw new EucalyptusCloudException(e);
		}
		if(osge != null) {
			throw osge;
		}
		throw new EucalyptusCloudException("Unable to obtain reply from dispatcher.");
	}

    private static S3Exception mapWalrusExceptionToS3Exception(EucalyptusCloudException ex) {
        if(ex instanceof S3Exception) {
            return (S3Exception) ex;
        } else {
            LOG.debug("Mapping excepion from Walrus to 500-InternalError", ex);
            InternalErrorException s3Ex = new InternalErrorException();
            s3Ex.initCause(ex);
            return s3Ex;
        }
    }

	/*
	 * -------------------------
	 * Service Operations
	 * ------------------------- 
	 */	


	@Override
	public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.ListAllMyBucketsType.class, com.eucalyptus.walrus.msgs.ListAllMyBucketsResponseType.class);
		} catch(EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	/*
	 * -------------------------
	 * Bucket Operations
	 * ------------------------- 
	 */		
	@Override
	public CreateBucketResponseType createBucket(CreateBucketType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.CreateBucketType.class, com.eucalyptus.walrus.msgs.CreateBucketResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}	
	}

	@Override
	public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.DeleteBucketType.class, com.eucalyptus.walrus.msgs.DeleteBucketResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}		
	}

	@Override
	public HeadBucketResponseType headBucket(HeadBucketType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.HeadBucketType.class, com.eucalyptus.walrus.msgs.HeadBucketResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}	
	}

	@Override
	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyType.class, com.eucalyptus.walrus.msgs.GetBucketAccessControlPolicyResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public ListBucketResponseType listBucket(ListBucketType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.ListBucketType.class, com.eucalyptus.walrus.msgs.ListBucketResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}
	
	@Override
	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(SetBucketAccessControlPolicyType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.SetRESTBucketAccessControlPolicyType.class, com.eucalyptus.walrus.msgs.SetRESTBucketAccessControlPolicyResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.GetBucketLocationType.class, com.eucalyptus.walrus.msgs.GetBucketLocationResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(SetBucketLoggingStatusType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.SetBucketLoggingStatusType.class, com.eucalyptus.walrus.msgs.SetBucketLoggingStatusResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(GetBucketLoggingStatusType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.GetBucketLoggingStatusType.class, com.eucalyptus.walrus.msgs.GetBucketLoggingStatusResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.GetBucketVersioningStatusType.class, com.eucalyptus.walrus.msgs.GetBucketVersioningStatusResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public SetBucketVersioningStatusResponseType setBucketVersioningStatus(SetBucketVersioningStatusType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.SetBucketVersioningStatusType.class, com.eucalyptus.walrus.msgs.SetBucketVersioningStatusResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public ListVersionsResponseType listVersions(ListVersionsType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.ListVersionsType.class, com.eucalyptus.walrus.msgs.ListVersionsResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public DeleteVersionResponseType deleteVersion(DeleteVersionType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.DeleteVersionType.class, com.eucalyptus.walrus.msgs.DeleteVersionResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	/*
	 * -------------------------
	 * Object Operations
	 * ------------------------- 
	 */
	
	//GET and PUT object are the same as the base S3ProviderClient.

	@Override
	public PostObjectResponseType postObject(PostObjectType request) throws S3Exception {
		throw new NotImplementedException("PostObject");
	}

	@Override
	public CopyObjectResponseType copyObject(CopyObjectType request) throws S3Exception {		
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.CopyObjectType.class, com.eucalyptus.walrus.msgs.CopyObjectResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.DeleteObjectType.class, com.eucalyptus.walrus.msgs.DeleteObjectResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyType.class, com.eucalyptus.walrus.msgs.GetObjectAccessControlPolicyResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}

	@Override
	public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(SetObjectAccessControlPolicyType request) throws S3Exception {
		try {
			return proxyRequest(request, com.eucalyptus.walrus.msgs.SetRESTObjectAccessControlPolicyType.class, com.eucalyptus.walrus.msgs.SetRESTObjectAccessControlPolicyResponseType.class);			
		} catch (EucalyptusCloudException e) {
			LOG.error("Error response from Walrus", e);
			throw mapWalrusExceptionToS3Exception(e);
		}
	}


}
