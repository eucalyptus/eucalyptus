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

package com.eucalyptus.objectstorage.providers.s3;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLoggingConfiguration;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.EmailAddressGrantee;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ListMultipartUploadsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.MultipartUpload;
import com.amazonaws.services.s3.model.MultipartUploadListing;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartListing;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SetBucketLoggingConfigurationRequest;
import com.amazonaws.services.s3.model.SetBucketVersioningConfigurationRequest;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.services.s3.model.VersionListing;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.exceptions.S3ExceptionMapper;
import com.eucalyptus.objectstorage.exceptions.s3.AccountProblemException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
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
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectExtendedType;
import com.eucalyptus.objectstorage.msgs.GetObjectResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectType;
import com.eucalyptus.objectstorage.msgs.HeadBucketResponseType;
import com.eucalyptus.objectstorage.msgs.HeadBucketType;
import com.eucalyptus.objectstorage.msgs.HeadObjectResponseType;
import com.eucalyptus.objectstorage.msgs.HeadObjectType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.InitiateMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsResponseType;
import com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType;
import com.eucalyptus.objectstorage.msgs.ListBucketResponseType;
import com.eucalyptus.objectstorage.msgs.ListBucketType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsResponseType;
import com.eucalyptus.objectstorage.msgs.ListMultipartUploadsType;
import com.eucalyptus.objectstorage.msgs.ListPartsResponseType;
import com.eucalyptus.objectstorage.msgs.ListPartsType;
import com.eucalyptus.objectstorage.msgs.ListVersionsResponseType;
import com.eucalyptus.objectstorage.msgs.ListVersionsType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders.ObjectStorageProviderClientProperty;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.OSGUtil;
import com.eucalyptus.objectstorage.client.OsgInternalS3Client;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.DeleteMarkerEntry;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Grantee;
import com.eucalyptus.storage.msgs.s3.Group;
import com.eucalyptus.storage.msgs.s3.Initiator;
import com.eucalyptus.storage.msgs.s3.KeyEntry;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.VersionEntry;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Base class for S3-api based backends. Uses the Amazon Java SDK as the client.
 * Can be extended for additional capabilities.
 *
 * The S3ProviderClient does IAM evaluation prior to dispatching requests to the backend and
 * will validate all results upon receipt of the response from the backend (e.g. for listing buckets).
 *
 * Current implementation maps all Euca credentials to a single backend s3 credential as configured in
 * {@link S3ProviderConfiguration}. The implication is that this provider will not enforce ACLs, policies,
 * or any separation between Euca-users.
 *
 */
@ObjectStorageProviderClientProperty("s3")
public class S3ProviderClient implements ObjectStorageProviderClient {
	private static final Logger LOG = Logger.getLogger(S3ProviderClient.class); 
	protected OsgInternalS3Client osgInternalS3Client = null;

	/**
	 * Returns a usable S3 Client configured to send requests to the currently configured
	 * endpoint with the currently configured credentials.
	 * @return
	 */
	protected AmazonS3Client getS3Client(User requestUser, String requestAWSAccessKeyId) throws InternalErrorException {
		//TODO: this should be enhanced to share clients/use a pool for efficiency.
		if (osgInternalS3Client == null) {
			synchronized(this) {		
				Protocol protocol = null;
				boolean useHttps = false;
				if(S3ProviderConfiguration.getS3UseHttps() != null && S3ProviderConfiguration.getS3UseHttps()) {
					useHttps = true;
				}
                AWSCredentials credentials;
                try {
                    credentials = mapCredentials(requestUser, requestAWSAccessKeyId);
                } catch(Exception e) {
                    LOG.error("Error mapping credentials for user " + (requestUser != null ? requestUser.getUserId() : "null") + " for walrus backend call.", e);
                    InternalErrorException ex = new InternalErrorException("Cannot construct s3client due to inability to map credentials for user: " +  (requestUser != null ? requestUser.getUserId() : "null"));
                    ex.initCause(e);
                    throw ex;
                }

				osgInternalS3Client = new OsgInternalS3Client(credentials, useHttps);
				osgInternalS3Client.setS3Endpoint(S3ProviderConfiguration.getS3Endpoint());
				osgInternalS3Client.setUsePathStyle(!S3ProviderConfiguration.getS3UseBackendDns());
				LOG.debug("Setting system property com.amazonaws.services.s3.disableGetObjectMD5Validation=true");
				System.setProperty("com.amazonaws.services.s3.disableGetObjectMD5Validation", Boolean.TRUE.toString());
			}
		}
		return osgInternalS3Client.getS3Client();
	}

	/**
	 * Returns the S3 ACL in euca object form. Does not modify the results,
	 * so owner information will be preserved.
	 * @param s3Acl
	 * @return
	 */
	protected static AccessControlPolicy sdkAclToEucaAcl(com.amazonaws.services.s3.model.AccessControlList s3Acl) {
		if(s3Acl == null) { return null; }		
		AccessControlPolicy acp = new AccessControlPolicy();

		acp.setOwner(new CanonicalUser(acp.getOwner().getID(), acp.getOwner().getDisplayName()));
		if(acp.getAccessControlList() == null) {
			acp.setAccessControlList(new AccessControlList());
		}
		Grantee grantee = null;
		for(com.amazonaws.services.s3.model.Grant g : s3Acl.getGrants()) {

			grantee = new Grantee();
			if(g.getGrantee() instanceof CanonicalGrantee) {
				grantee.setCanonicalUser(new CanonicalUser(g.getGrantee().getIdentifier(),((CanonicalGrantee)g.getGrantee()).getDisplayName()));
			} else if(g.getGrantee() instanceof GroupGrantee) {
				grantee.setGroup(new Group(g.getGrantee().getIdentifier()));
			} else if(g.getGrantee() instanceof EmailAddressGrantee) {
				grantee.setEmailAddress(g.getGrantee().getIdentifier());
			}

			acp.getAccessControlList().getGrants().add(new Grant(grantee, g.getPermission().toString()));
		}

		return acp;
	}

	/**
	 * Maps the request credentials to another set of credentials. This implementation maps
	 * all Eucalyptus credentials to a single s3/backend credential.
	 * 
	 * @param requestUser The Eucalyptus user that generated the request
	 * @param requestAWSAccessKeyId The access key id used for this request
	 * @return a BasicAWSCredentials object initialized with the credentials to use
	 * @throws NoSuchElementException
	 * @throws IllegalArgumentException
	 */
	protected BasicAWSCredentials mapCredentials(User requestUser, String requestAWSAccessKeyId) throws AuthException, IllegalArgumentException {
		return new BasicAWSCredentials(S3ProviderConfiguration.getS3AccessKey(), S3ProviderConfiguration.getS3SecretKey());
	}

	protected ObjectMetadata getS3ObjectMetadata(PutObjectType request) {
		ObjectMetadata meta = new ObjectMetadata();
		if(request.getMetaData() != null) {
			for(MetaDataEntry m : request.getMetaData()) {				
				meta.addUserMetadata(m.getName(), m.getValue());
			}
		}

		if(!Strings.isNullOrEmpty(request.getContentLength())) {
			meta.setContentLength(Long.parseLong(request.getContentLength()));
		}

		if(!Strings.isNullOrEmpty(request.getContentMD5())) {		
			meta.setContentMD5(request.getContentMD5());
		}

		if(!Strings.isNullOrEmpty(request.getContentType())) {
			meta.setContentType(request.getContentType());
		}

		return meta;
	}

	@Override
	public void initialize() throws EucalyptusCloudException {
		LOG.debug("Initializing");
		check();
		LOG.debug("Initialization completed successfully");		
	}

	@Override
	public void check() throws EucalyptusCloudException {
		LOG.debug("Checking");
        Socket s = null;
		try {
            s = new Socket(S3ProviderConfiguration.getS3EndpointHost(), S3ProviderConfiguration.getS3EndpointPort());
		} catch (UnknownHostException e) {
			//it is safe to do this because we won't try to execute an operation until enable returns successfully.
			osgInternalS3Client = null;
			throw new EucalyptusCloudException("Host Exception. Unable to connect to S3 Endpoint: " + S3ProviderConfiguration.getS3Endpoint() + ". Please check configuration and network connection");
		} catch (IOException e) {
			osgInternalS3Client = null;
			throw new EucalyptusCloudException("Unable to connect to S3 Endpoint: " + S3ProviderConfiguration.getS3Endpoint() + ". Please check configuration and network connection");
		} finally {
            try {
                if( s != null && !s.isClosed()) {
                    s.close();
                }
            } catch(IOException e) {
                LOG.warn("Failed closing socked used to check remote S3 endpoint",e);
            }
        }
		LOG.debug("Check completed successfully");		
	}

	@Override
	public void checkPreconditions() throws EucalyptusCloudException {
		LOG.debug("Checking preconditions");		
		LOG.debug("Check preconditions completed successfully");
	}

	@Override
	public void start() throws EucalyptusCloudException {
		LOG.debug("Starting");
		LOG.debug("Start completed successfully");		
	}

	@Override
	public void stop() throws EucalyptusCloudException {
		LOG.debug("Stopping");
		//Force a new load of this on startup.
		this.osgInternalS3Client = null;
		LOG.debug("Stop completed successfully");		
	}

	@Override
	public void enable() throws EucalyptusCloudException {
		LOG.debug("Enabling");
		LOG.debug("Enable completed successfully");
	}

	@Override
	public void disable() throws EucalyptusCloudException {
		LOG.debug("Disabling");		
		LOG.debug("Disable completed successfully");
	}

	protected CanonicalUser getCanonicalUser(User usr) {
        try {
            return new CanonicalUser(usr.getAccount().getCanonicalId(), usr.getAccount().getName());
        } catch(AuthException e) {
            LOG.debug("Exception getting the canonicalId and display name for the user: " + usr.getName(), e);
            return new CanonicalUser();
        }
	}

	/*
	 * TODO: add multi-account support on backend and then this can be a pass-thru to backend for bucket listing.
	 * Multiplexing a single eucalyptus account on the backend means we have to track all of the user buckets ourselves
	 * (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageProviderClient#listAllMyBuckets(com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType)
	 */
	@Override
	public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws S3Exception {
		ListAllMyBucketsResponseType reply = request.getReply();
		try {
            User requestUser = getRequestUser(request);
			//The euca-types
			ListAllMyBucketsList myBucketList = new ListAllMyBucketsList();
			myBucketList.setBuckets(new ArrayList<BucketListEntry>());


			//The s3 client types
			AmazonS3Client s3Client = this.getS3Client(requestUser, request.getAccessKeyID());
			ListBucketsRequest listRequest = new ListBucketsRequest();

			//Map s3 client result to euca response message
			List<Bucket> result = s3Client.listBuckets(listRequest);
			for(Bucket b : result) {
				myBucketList.getBuckets().add(new BucketListEntry(b.getName(), DateFormatter.dateToHeaderFormattedString(b.getCreationDate())));
			}

			reply.setBucketList(myBucketList);
        } catch(AmazonServiceException ex) {
            LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
        }
		return reply;		
	}

	/**
	 * Handles a HEAD request to the bucket. Just returns 200ok if bucket exists and user has access. Otherwise
	 * returns 404 if not found or 403 if no accesss.
	 * @param request
	 * @return
	 * @throws S3Exception
	 */
	@Override
	public HeadBucketResponseType headBucket(HeadBucketType request) throws S3Exception {
		HeadBucketResponseType reply = (HeadBucketResponseType) request.getReply();
		User requestUser = getRequestUser(request);

		// call the storage manager to save the bucket to disk
		try {
			AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
			com.amazonaws.services.s3.model.AccessControlList responseList = s3Client.getBucketAcl(request.getBucket());
			reply.setBucket(request.getBucket());
		} catch(AmazonServiceException ex) {
			LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
        }

		return reply;		

	}

	@Override
	public CreateBucketResponseType createBucket(CreateBucketType request) throws S3Exception {
		CreateBucketResponseType reply = (CreateBucketResponseType) request.getReply();
		User requestUser = getRequestUser(request);
		
		try {
			AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
			Bucket responseBucket = s3Client.createBucket(request.getBucket());
			//Save the owner info in response?
			reply.setBucket(request.getBucket());
			reply.setStatus(HttpResponseStatus.OK);
			reply.setStatusMessage("OK");
		} catch(AmazonServiceException ex) {
            LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
		}

		return reply;		
	}



    @Override
	public DeleteBucketResponseType deleteBucket(DeleteBucketType request) throws S3Exception {
		DeleteBucketResponseType reply = (DeleteBucketResponseType) request.getReply();
		User requestUser = getRequestUser(request);
		
		// call the storage manager to save the bucket to disk
		try {
			AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
			s3Client.deleteBucket(request.getBucket());
		} catch(AmazonServiceException ex) {
            LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
        }

		return reply;		
	}

    /**
     * Should remove this, mostly pointless
     * @param request
     * @return
     */
    private static User getRequestUser(ObjectStorageRequestType request) {
        return request.getUser();
    }

    @Override
	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(
			GetBucketAccessControlPolicyType request)
					throws S3Exception {
		GetBucketAccessControlPolicyResponseType reply = (GetBucketAccessControlPolicyResponseType) request.getReply();
		User requestUser = getRequestUser(request);
		
		try {
			AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
			com.amazonaws.services.s3.model.AccessControlList acl = s3Client.getBucketAcl(request.getBucket());
			reply.setAccessControlPolicy(sdkAclToEucaAcl(acl));			
		} catch(AmazonServiceException ex) {
			LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
		}

		return reply;
	}

	@Override
	public PutObjectResponseType putObject(PutObjectType request, InputStream inputData) throws S3Exception {
		try {
			User requestUser = getRequestUser(request);
			AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
			PutObjectResult result = null;
            ObjectMetadata metadata = getS3ObjectMetadata(request);
            //Set the acl to private.
            PutObjectRequest putRequest = new PutObjectRequest(request.getBucket(),
                    request.getKey(),
                    inputData,
                    metadata).withCannedAcl(CannedAccessControlList.Private);
            result = s3Client.putObject(putRequest);

			PutObjectResponseType reply = request.getReply();
			if(result == null) {
				throw new InternalErrorException("Null result from backend");
			} else {
				reply.setEtag(result.getETag());
				reply.setVersionId(result.getVersionId());
				reply.setLastModified(new Date());
			}
			return reply;
		} catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
		}
	}

	@Override
	public PostObjectResponseType postObject(PostObjectType request)
			throws S3Exception {
		throw new NotImplementedException("PostObject");
	}

	@Override
	public DeleteObjectResponseType deleteObject(DeleteObjectType request) throws S3Exception {
		try {
			User requestUser = getRequestUser(request);

            DeleteObjectResponseType reply = (DeleteObjectResponseType) request.getReply();
            reply.setStatus(HttpResponseStatus.NO_CONTENT);
            reply.setStatusMessage("NO CONTENT");

            AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
            s3Client.deleteObject(request.getBucket(), request.getKey());
            return reply;
        } catch(AmazonServiceException ex) {
            LOG.debug("Error from backend", ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
        }
	}

	@Override
	public ListBucketResponseType listBucket(ListBucketType request) throws S3Exception {
		ListBucketResponseType reply = (ListBucketResponseType) request.getReply();
		try {
			User requestUser = getRequestUser(request);
			
			AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
			ListObjectsRequest listRequest = new ListObjectsRequest();
			listRequest.setBucketName(request.getBucket());
			listRequest.setDelimiter(Strings.isNullOrEmpty(request.getDelimiter()) ? null : request.getDelimiter());
			listRequest.setMarker(Strings.isNullOrEmpty(request.getMarker()) ? null : request.getMarker());
			listRequest.setMaxKeys((request.getMaxKeys() == null ? null : Integer.parseInt(request.getMaxKeys())));
			listRequest.setPrefix(Strings.isNullOrEmpty(request.getPrefix()) ? null : request.getPrefix());

			ObjectListing response = s3Client.listObjects(listRequest);

			/* Non-optional, must have non-null values */
            reply.setName(request.getBucket());
            reply.setMaxKeys(response.getMaxKeys());
            reply.setMarker(response.getMarker() == null ? "" : response.getMarker());
            reply.setPrefix(response.getPrefix() == null ? "" : response.getPrefix());
            reply.setIsTruncated(response.isTruncated());

			/* Optional */
            reply.setNextMarker(response.getNextMarker());
            reply.setDelimiter(response.getDelimiter());
            if(reply.getContents() == null) {
                reply.setContents(new ArrayList<ListEntry>());
            }
            if(reply.getCommonPrefixesList() == null) {
                reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());
            }

            for(S3ObjectSummary obj : response.getObjectSummaries()) {
                //Add entry, note that the canonical user is set based on requesting user, not returned user
                reply.getContents().add(new ListEntry(
                        obj.getKey(),
                        DateFormatter.dateToHeaderFormattedString(obj.getLastModified()),
                        obj.getETag(),
                        obj.getSize(),
                        getCanonicalUser(requestUser),
                        obj.getStorageClass()));
            }

            if(response.getCommonPrefixes() != null && response.getCommonPrefixes().size() > 0) {
                reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());

                for(String s : response.getCommonPrefixes()) {
                    reply.getCommonPrefixesList().add(new CommonPrefixesEntry(s));
                }
            }

            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }

    @Override
    public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws S3Exception {
        GetObjectAccessControlPolicyResponseType reply = (GetObjectAccessControlPolicyResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            com.amazonaws.services.s3.model.AccessControlList acl = s3Client.getObjectAcl(request.getBucket(),request.getKey(), request.getVersionId());
            reply.setAccessControlPolicy(sdkAclToEucaAcl(acl));
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(
            SetBucketAccessControlPolicyType request)
            throws S3Exception {
        throw new NotImplementedException("?acl");
    }

    @Override
    public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(
            SetObjectAccessControlPolicyType request)
            throws S3Exception {
        throw new NotImplementedException("?acl");
    }

    protected void populateResponseMetadata(final ObjectStorageDataResponseType reply, final ObjectMetadata metadata) {
        reply.setSize(metadata.getContentLength());
        reply.setContentDisposition(metadata.getContentDisposition());
        reply.setContentType(metadata.getContentType());
        reply.setEtag(metadata.getETag());
        reply.setLastModified(metadata.getLastModified());

        if(metadata.getUserMetadata() != null && metadata.getUserMetadata().size() > 0) {
            if(reply.getMetaData() == null) reply.setMetaData(new ArrayList<MetaDataEntry>());

            for(String k : metadata.getUserMetadata().keySet()) {
                reply.getMetaData().add(new MetaDataEntry(k, metadata.getUserMetadata().get(k)));
            }
        }

    }
    @Override
    public GetObjectResponseType getObject(final GetObjectType request) throws S3Exception {
        User requestUser = getRequestUser(request);

        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        GetObjectRequest getRequest = new GetObjectRequest(request.getBucket(), request.getKey());
        try {
            GetObjectResponseType reply = (GetObjectResponseType)request.getReply();
            S3Object response = null;
            response = s3Client.getObject(getRequest);
            populateResponseMetadata((ObjectStorageDataResponseType)reply, response.getObjectMetadata());
            reply.setDataInputStream(response.getObjectContent());
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }


    @Override
    public GetObjectExtendedResponseType getObjectExtended(final GetObjectExtendedType request) throws S3Exception {
        User requestUser = getRequestUser(request);

        Boolean getMetaData = request.getGetMetaData();
        Boolean inlineData = request.getInlineData();
        Long byteRangeStart = request.getByteRangeStart();
        Long byteRangeEnd = request.getByteRangeEnd();
        Date ifModifiedSince = request.getIfModifiedSince();
        Date ifUnmodifiedSince = request.getIfUnmodifiedSince();
        String ifMatch = request.getIfMatch();
        String ifNoneMatch = request.getIfNoneMatch();

        GetObjectRequest getRequest = new GetObjectRequest(request.getBucket(), request.getKey());
        if(byteRangeStart == null) {
            byteRangeStart = 0L;
        }
        if(byteRangeEnd != null) {
            getRequest.setRange(byteRangeStart, byteRangeEnd);
        }
        if(getMetaData != null) {
            //Get object metadata
        }
        if(ifModifiedSince != null) {
            getRequest.setModifiedSinceConstraint(ifModifiedSince);
        }
        if(ifUnmodifiedSince != null) {
            getRequest.setUnmodifiedSinceConstraint(ifUnmodifiedSince);
        }
        if(ifMatch != null) {
            List matchList = new ArrayList();
            matchList.add(ifMatch);
            getRequest.setMatchingETagConstraints(matchList);
        }
        if(ifNoneMatch != null) {
            List nonMatchList = new ArrayList();
            nonMatchList.add(ifNoneMatch);
            getRequest.setNonmatchingETagConstraints(nonMatchList);
        }
        try {
            AmazonS3Client s3Client = this.getS3Client(requestUser, requestUser.getUserId());
            S3Object response = s3Client.getObject(getRequest);

            GetObjectExtendedResponseType reply = request.getReply();
            populateResponseMetadata(reply, response.getObjectMetadata());
            reply.setDataInputStream(response.getObjectContent());
            reply.setByteRangeStart(request.getByteRangeStart());
            reply.setByteRangeEnd(request.getByteRangeEnd());
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }

    @Override
    public GetBucketLocationResponseType getBucketLocation(
            GetBucketLocationType request) throws S3Exception {
        GetBucketLocationResponseType reply = (GetBucketLocationResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            String bucketLocation = s3Client.getBucketLocation(request.getBucket());
            reply.setLocationConstraint(bucketLocation);
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public CopyObjectResponseType copyObject(CopyObjectType request)
            throws S3Exception {
        CopyObjectResponseType reply = (CopyObjectResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        String sourceBucket = request.getSourceBucket();
        String sourceKey = request.getSourceObject();
        String sourceVersionId = request.getSourceVersionId();
        String destinationBucket = request.getDestinationBucket();
        String destinationKey = request.getDestinationObject();
        String copyIfMatch = request.getCopySourceIfMatch();
        String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
        Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
        Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();
        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(sourceBucket, sourceKey, sourceVersionId, destinationBucket, destinationKey);
            copyRequest.setModifiedSinceConstraint(copyIfModifiedSince);
            copyRequest.setUnmodifiedSinceConstraint(copyIfUnmodifiedSince);
            if (copyIfMatch != null) {
                List<String> copyIfMatchConstraint = new ArrayList<String>();
                copyIfMatchConstraint.add(copyIfMatch);
                copyRequest.setMatchingETagConstraints(copyIfMatchConstraint);
            }
            if (copyIfNoneMatch != null) {
                List<String> copyIfNoneMatchConstraint = new ArrayList<String>();
                copyIfNoneMatchConstraint.add(copyIfNoneMatch);
                copyRequest.setNonmatchingETagConstraints(copyIfNoneMatchConstraint);
            }
            //TODO: Need to set canned ACL if specified
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            CopyObjectResult result = s3Client.copyObject(copyRequest);
            reply.setEtag(result.getETag());
            reply.setLastModified(DateFormatter.dateToListingFormattedString(result.getLastModifiedDate()));
            String destinationVersionId = result.getVersionId();
            if (destinationVersionId != null) {
                reply.setCopySourceVersionId(sourceVersionId);
                reply.setVersionId(destinationVersionId);
            }
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
        return reply;

    }

    /* NOTE: bucket logging grants don't work because there is no way to specify them via the
     * S3 Java SDK. Need to add that functionality ourselves if we want it on the backend directly
     *
     * (non-Javadoc)
     * @see com.eucalyptus.objectstorage.ObjectStorageProviderClient#setBucketLoggingStatus(com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType)
     */
    @Override
    public SetBucketLoggingStatusResponseType setBucketLoggingStatus(
            SetBucketLoggingStatusType request) throws S3Exception {
        SetBucketLoggingStatusResponseType reply = (SetBucketLoggingStatusResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            BucketLoggingConfiguration config = new BucketLoggingConfiguration();
            LoggingEnabled requestConfig = request.getLoggingEnabled();
            config.setDestinationBucketName(requestConfig == null ? null : requestConfig.getTargetBucket());
            config.setLogFilePrefix(requestConfig == null ? null : requestConfig.getTargetPrefix());

            SetBucketLoggingConfigurationRequest loggingRequest = new SetBucketLoggingConfigurationRequest(request.getBucket(), config);
            s3Client.setBucketLoggingConfiguration(loggingRequest);
            reply.setStatus(HttpResponseStatus.OK);
            reply.setStatusMessage("OK");
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public GetBucketLoggingStatusResponseType getBucketLoggingStatus(
            GetBucketLoggingStatusType request) throws S3Exception {
        GetBucketLoggingStatusResponseType reply = (GetBucketLoggingStatusResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            BucketLoggingConfiguration loggingConfig = s3Client.getBucketLoggingConfiguration(request.getBucket());
            LoggingEnabled loggingEnabled = new LoggingEnabled();
            if(loggingConfig == null || !loggingConfig.isLoggingEnabled()) {
                //Do nothing, logging is disabled
            } else {
                //S3 SDK does not provide a way to fetch the grants on the destination logging
                loggingEnabled.setTargetBucket(loggingConfig.getDestinationBucketName());
                loggingEnabled.setTargetPrefix(loggingConfig.getLogFilePrefix());
            }
            reply.setLoggingEnabled(loggingEnabled);
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request)
            throws S3Exception {
        GetBucketVersioningStatusResponseType reply = (GetBucketVersioningStatusResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            BucketVersioningConfiguration versioning = s3Client.getBucketVersioningConfiguration(request.getBucket());
            reply.setVersioningStatus(versioning.getStatus());
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public SetBucketVersioningStatusResponseType setBucketVersioningStatus(
            SetBucketVersioningStatusType request)
            throws S3Exception {
        SetBucketVersioningStatusResponseType reply = (SetBucketVersioningStatusResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            BucketVersioningConfiguration config = new BucketVersioningConfiguration().withStatus(request.getVersioningStatus());
            SetBucketVersioningConfigurationRequest configRequest = new SetBucketVersioningConfigurationRequest(request.getBucket(), config);
            s3Client.setBucketVersioningConfiguration(configRequest);
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }


    @Override
    public ListVersionsResponseType listVersions(ListVersionsType request) throws S3Exception {
        ListVersionsResponseType reply = (ListVersionsResponseType) request.getReply();
        User requestUser = getRequestUser(request);

        try {
            AmazonS3Client s3Client = getS3Client(requestUser, requestUser.getUserId());
            ListVersionsRequest listVersionsRequest = new ListVersionsRequest(request.getBucket(),
                    request.getPrefix(),
                    request.getKeyMarker(),
                    request.getVersionIdMarker(),
                    request.getDelimiter(), Integer.parseInt(request.getMaxKeys()));
            VersionListing result = s3Client.listVersions(listVersionsRequest);

            CanonicalUser owner = null;
            try {
                owner = AclUtils.buildCanonicalUser(requestUser.getAccount());
            } catch(AuthException e) {
                LOG.error("Error getting request user's account during bucket version listing",e);
                owner = null;
                throw new AccountProblemException("Account for user " + requestUser.getUserId());
            }

            //Populate result to euca
            reply.setBucket(request.getBucket());
            reply.setMaxKeys(result.getMaxKeys());
            reply.setDelimiter(result.getDelimiter());
            reply.setNextKeyMarker(result.getNextKeyMarker());
            reply.setNextVersionIdMarker(result.getNextVersionIdMarker());
            reply.setIsTruncated(result.isTruncated());
            reply.setVersionIdMarker(result.getVersionIdMarker());
            reply.setKeyMarker(result.getKeyMarker());

            if(result.getCommonPrefixes() != null && result.getCommonPrefixes().size() > 0) {
                reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());

                for(String s : result.getCommonPrefixes()) {
                    reply.getCommonPrefixesList().add(new CommonPrefixesEntry(s));
                }
            }

            ArrayList<KeyEntry> versions = new ArrayList<KeyEntry>();
            VersionEntry v = null;
            DeleteMarkerEntry d = null;
            for(S3VersionSummary summary: result.getVersionSummaries()) {
                if(!summary.isDeleteMarker()) {
                    v = new VersionEntry();
                    v.setKey(summary.getKey());
                    v.setVersionId(summary.getVersionId());
                    v.setLastModified(DateFormatter.dateToHeaderFormattedString(summary.getLastModified()));
                    v.setEtag(summary.getETag());
                    v.setIsLatest(summary.isLatest());
                    v.setOwner(owner);
                    v.setSize(summary.getSize());
                    versions.add(v);
                } else {
                    d = new DeleteMarkerEntry();
                    d.setIsLatest(summary.isLatest());
                    d.setKey(summary.getKey());
                    d.setLastModified(DateFormatter.dateToHeaderFormattedString(summary.getLastModified()));
                    d.setOwner(owner);
                    d.setVersionId(summary.getVersionId());
                    versions.add(d);
                }
            }
            //Again, this is wrong, should be a single listing
            reply.setKeyEntries(versions);
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

        return reply;
    }

    @Override
    public DeleteVersionResponseType deleteVersion(DeleteVersionType request) throws S3Exception {
        try {
            User requestUser = getRequestUser(request);

            AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
            s3Client.deleteVersion(request.getBucket(), request.getKey(), request.getVersionId());
            DeleteVersionResponseType reply = (DeleteVersionResponseType) request.getReply();
            reply.setStatus(HttpResponseStatus.NO_CONTENT);
            reply.setStatusMessage("NO CONTENT");
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }

    @Override
    public HeadObjectResponseType headObject(final HeadObjectType request)
            throws S3Exception {
        User requestUser = getRequestUser(request);

        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        GetObjectMetadataRequest getMetadataRequest = new GetObjectMetadataRequest(request.getBucket(), request.getKey());
        getMetadataRequest.setVersionId(request.getVersionId());
        try {
            ObjectMetadata metadata = null;
            metadata = s3Client.getObjectMetadata(getMetadataRequest);

            HeadObjectResponseType reply = (HeadObjectResponseType)request.getReply();
            populateResponseMetadata((ObjectStorageDataResponseType)reply, metadata);
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }

    @Override
    public InitiateMultipartUploadResponseType initiateMultipartUpload(
            InitiateMultipartUploadType request)
            throws S3Exception {
        InitiateMultipartUploadResponseType reply = (InitiateMultipartUploadResponseType) request.getReply();
        User requestUser = getRequestUser(request);
        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        String bucketName = request.getBucket();
        String key = request.getKey();
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
        ObjectMetadata metadata = new ObjectMetadata();
        for(MetaDataEntry meta : request.getMetaData() ) {
            metadata.addUserMetadata(meta.getName(), meta.getValue());
        }

        initiateMultipartUploadRequest.setObjectMetadata(metadata);
        try {
            InitiateMultipartUploadResult result = s3Client.initiateMultipartUpload(initiateMultipartUploadRequest);
            reply.setUploadId(result.getUploadId());
            reply.setBucket(bucketName);
            reply.setKey(key);
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }

    @Override
    public UploadPartResponseType uploadPart(UploadPartType request, InputStream dataContent)
            throws S3Exception {
        String bucketName = request.getBucket();
        String key = request.getKey();

        try {
            User requestUser = getRequestUser(request);

            AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
            UploadPartResult result = null;
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(dataContent);
            uploadPartRequest.setUploadId(request.getUploadId());
            uploadPartRequest.setPartNumber(Integer.valueOf(request.getPartNumber()));
            uploadPartRequest.setMd5Digest(request.getContentMD5());
            uploadPartRequest.setPartSize(Long.valueOf(request.getContentLength()));
            try {
                result = s3Client.uploadPart(uploadPartRequest);
            } catch(AmazonServiceException e) {
                LOG.debug("Error from backend", e);
                throw S3ExceptionMapper.fromAWSJavaSDK(e);
            }
            UploadPartResponseType reply = (UploadPartResponseType) request.getReply();
            reply.setEtag(result.getETag());
            reply.setLastModified(new Date());
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }

    }

    @Override
    public CompleteMultipartUploadResponseType completeMultipartUpload(
            CompleteMultipartUploadType request)
            throws S3Exception {
        CompleteMultipartUploadResponseType reply = (CompleteMultipartUploadResponseType) request.getReply();
        User requestUser = getRequestUser(request);
        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        String bucketName = request.getBucket();
        String key = request.getKey();
        String uploadId = request.getUploadId();
        List<Part> parts = request.getParts();
        List<PartETag> partETags = new ArrayList<PartETag>();
        for (Part part : parts) {
            PartETag partETag = new PartETag(part.getPartNumber(), part.getEtag());
            partETags.add(partETag);
        }
        CompleteMultipartUploadRequest multipartRequest = new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
        try {
            CompleteMultipartUploadResult result = s3Client.completeMultipartUpload(multipartRequest);
            reply.setEtag(result.getETag());
            reply.setBucket(bucketName);
            reply.setKey(key);
            reply.setLocation(result.getLocation());
            reply.setLastModified(new Date());
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
        return reply;
    }

    @Override
    public AbortMultipartUploadResponseType abortMultipartUpload(
            AbortMultipartUploadType request) throws S3Exception {
        AbortMultipartUploadResponseType reply = (AbortMultipartUploadResponseType) request.getReply();
        User requestUser = getRequestUser(request);
        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        String bucketName = request.getBucket();
        String key = request.getKey();
        String uploadId = request.getUploadId();
        AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucketName, key, uploadId);
        try {
            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
        return reply;
    }

    @Override
    public ListPartsResponseType listParts(ListPartsType request)
            throws S3Exception {
        ListPartsResponseType reply = (ListPartsResponseType) request.getReply();
        User requestUser = getRequestUser(request);
        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        String bucketName = request.getBucket();
        String key = request.getKey();
        String uploadId = request.getUploadId();
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
        if (request.getMaxParts() != null) {
            listPartsRequest.setMaxParts(request.getMaxParts());
        }
        if(request.getPartNumberMarker() != null) {
            listPartsRequest.setPartNumberMarker(request.getPartNumberMarker());
        }
        try {
            PartListing listing = s3Client.listParts(listPartsRequest);
            reply.setBucket(bucketName);
            reply.setKey(key);
            reply.setUploadId(uploadId);
            Initiator initiator = new Initiator(
                    listing.getInitiator().getId(),
                    listing.getInitiator().getDisplayName());
            reply.setInitiator(initiator);
            CanonicalUser owner = new CanonicalUser(
                    listing.getOwner().getId(),
                    listing.getOwner().getDisplayName());
            reply.setOwner(owner);
            reply.setStorageClass(listing.getStorageClass());
            reply.setPartNumberMarker(listing.getPartNumberMarker());
            reply.setNextPartNumberMarker(listing.getNextPartNumberMarker());
            reply.setMaxParts(listing.getMaxParts());
            reply.setIsTruncated(listing.isTruncated());
            List<PartSummary> parts = listing.getParts();
            List<Part> replyParts = reply.getParts();
            for(PartSummary part : parts) {
                replyParts.add(new Part(
                        part.getPartNumber(),
                        part.getETag(),
                        part.getLastModified(),
                        part.getSize()
                ));
            }
        } catch(AmazonServiceException ex) {
            LOG.debug("Got service error from backend: " + ex.getMessage(), ex);
            throw S3ExceptionMapper.fromAWSJavaSDK(ex);
        }

        return reply;
    }

    @Override
    public ListMultipartUploadsResponseType listMultipartUploads(
            ListMultipartUploadsType request) throws S3Exception {
        ListMultipartUploadsResponseType reply = (ListMultipartUploadsResponseType) request.getReply();
        User requestUser = getRequestUser(request);
        AmazonS3Client s3Client = getS3Client(requestUser, request.getAccessKeyID());
        String bucketName = request.getBucket();
        ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
        listMultipartUploadsRequest.setMaxUploads(request.getMaxUploads());
        listMultipartUploadsRequest.setKeyMarker(request.getKeyMarker());
        listMultipartUploadsRequest.setDelimiter(request.getDelimiter());
        listMultipartUploadsRequest.setPrefix(request.getPrefix());
        listMultipartUploadsRequest.setUploadIdMarker(request.getUploadIdMarker());
        try {
            MultipartUploadListing listing = s3Client.listMultipartUploads(listMultipartUploadsRequest);
            reply.setBucket(listing.getBucketName());
            reply.setKeyMarker(listing.getKeyMarker());
            reply.setUploadIdMarker(listing.getUploadIdMarker());
            reply.setNextKeyMarker(listing.getNextKeyMarker());
            reply.setNextUploadIdMarker(listing.getNextUploadIdMarker());
            reply.setMaxUploads(listing.getMaxUploads());
            reply.setIsTruncated(listing.isTruncated());
            reply.setPrefix(listing.getPrefix());
            reply.setDelimiter(listing.getDelimiter());

            List<String> commonPrefixes = listing.getCommonPrefixes();
            List<MultipartUpload> multipartUploads = listing.getMultipartUploads();

            List<com.eucalyptus.storage.msgs.s3.Upload> uploads = reply.getUploads();
            List<CommonPrefixesEntry> prefixes = reply.getCommonPrefixes();

            for(MultipartUpload multipartUpload : multipartUploads) {
                uploads.add(new com.eucalyptus.storage.msgs.s3.Upload(multipartUpload.getKey(),
                        multipartUpload.getUploadId(),
                        new Initiator(multipartUpload.getInitiator().getId(),
                                multipartUpload.getInitiator().getDisplayName()),
                        new CanonicalUser(multipartUpload.getOwner().getId(),
                                multipartUpload.getOwner().getDisplayName()),
                        multipartUpload.getStorageClass(),
                        multipartUpload.getInitiated()
                ));
            }
            for(String commonPrefix : commonPrefixes) {
                prefixes.add(new CommonPrefixesEntry(commonPrefix));
            }
            return reply;
        } catch(AmazonServiceException e) {
            LOG.debug("Error from backend", e);
            throw S3ExceptionMapper.fromAWSJavaSDK(e);
        }
    }
}
