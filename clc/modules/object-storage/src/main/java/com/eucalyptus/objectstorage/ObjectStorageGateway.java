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

package com.eucalyptus.objectstorage;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.Topology;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.configurable.PropertyDirectory;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.NoSuchContextException;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.objectstorage.auth.OsgAuthorizationHandler;
import com.eucalyptus.objectstorage.bittorrent.Tracker;
import com.eucalyptus.objectstorage.entities.Bucket;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.ObjectStorageGlobalConfiguration;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.entities.S3AccessControlledEntity;
import com.eucalyptus.objectstorage.exceptions.IllegalResourceStateException;
import com.eucalyptus.objectstorage.exceptions.MetadataOperationFailureException;
import com.eucalyptus.objectstorage.exceptions.NoSuchEntityException;
import com.eucalyptus.objectstorage.exceptions.s3.AccessDeniedException;
import com.eucalyptus.objectstorage.exceptions.s3.AccountProblemException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketAlreadyExistsException;
import com.eucalyptus.objectstorage.exceptions.s3.BucketNotEmptyException;
import com.eucalyptus.objectstorage.exceptions.s3.IllegalVersioningConfigurationException;
import com.eucalyptus.objectstorage.exceptions.s3.InlineDataTooLargeException;
import com.eucalyptus.objectstorage.exceptions.s3.InternalErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidArgumentException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidBucketNameException;
import com.eucalyptus.objectstorage.exceptions.s3.InvalidBucketStateException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedACLErrorException;
import com.eucalyptus.objectstorage.exceptions.s3.MalformedXMLException;
import com.eucalyptus.objectstorage.exceptions.s3.MissingContentLengthException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchBucketException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchKeyException;
import com.eucalyptus.objectstorage.exceptions.s3.NoSuchUploadException;
import com.eucalyptus.objectstorage.exceptions.s3.NotImplementedException;
import com.eucalyptus.objectstorage.exceptions.s3.PreconditionFailedException;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.exceptions.s3.TooManyBucketsException;
import com.eucalyptus.objectstorage.metadata.BucketNameValidatorRepo;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.AbortMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadResponseType;
import com.eucalyptus.objectstorage.msgs.CompleteMultipartUploadType;
import com.eucalyptus.objectstorage.msgs.CopyObjectResponseType;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.CreateBucketResponseType;
import com.eucalyptus.objectstorage.msgs.CreateBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketLifecycleType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteBucketType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteVersionType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.GetBucketLifecycleType;
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
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationType;
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
import com.eucalyptus.objectstorage.msgs.ObjectStorageDataGetResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageRequestType;
import com.eucalyptus.objectstorage.msgs.PostObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PostObjectType;
import com.eucalyptus.objectstorage.msgs.PutObjectResponseType;
import com.eucalyptus.objectstorage.msgs.PutObjectType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.SetBucketLifecycleResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLifecycleType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusResponseType;
import com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationResponseType;
import com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationType;
import com.eucalyptus.objectstorage.msgs.UploadPartResponseType;
import com.eucalyptus.objectstorage.msgs.UploadPartType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviders;
import com.eucalyptus.objectstorage.util.AclUtils;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.reporting.event.S3ObjectEvent;
import com.eucalyptus.storage.common.DateFormatter;
import com.eucalyptus.storage.msgs.s3.AccessControlList;
import com.eucalyptus.storage.msgs.s3.AccessControlPolicy;
import com.eucalyptus.storage.msgs.s3.BucketListEntry;
import com.eucalyptus.storage.msgs.s3.CanonicalUser;
import com.eucalyptus.storage.msgs.s3.CommonPrefixesEntry;
import com.eucalyptus.storage.msgs.s3.Grant;
import com.eucalyptus.storage.msgs.s3.Initiator;
import com.eucalyptus.storage.msgs.s3.LifecycleConfiguration;
import com.eucalyptus.storage.msgs.s3.LifecycleRule;
import com.eucalyptus.storage.msgs.s3.ListAllMyBucketsList;
import com.eucalyptus.storage.msgs.s3.ListEntry;
import com.eucalyptus.storage.msgs.s3.LoggingEnabled;
import com.eucalyptus.storage.msgs.s3.Part;
import com.eucalyptus.storage.msgs.s3.TargetGrants;
import com.eucalyptus.storage.msgs.s3.Upload;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Strings;
import edu.ucsb.eucalyptus.msgs.ComponentProperty;
import edu.ucsb.eucalyptus.util.SystemUtil;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

/**
 * Operation handler for the ObjectStorageGateway. Main point of entry
 * This class handles user and system requests.
 *
 */
public class ObjectStorageGateway implements ObjectStorageService {
    private static Logger LOG = Logger.getLogger( ObjectStorageGateway.class );

    private static ObjectStorageProviderClient ospClient = null;
    protected static final String USR_EMAIL_KEY = "email";//lookup for account admins email

	public ObjectStorageGateway() {}

	public static void checkPreconditions() throws EucalyptusCloudException, ExecutionException {
		LOG.debug("Checking ObjectStorageGateway preconditions");
		LOG.debug("ObjectStorageGateway Precondition check complete");
	}

	public static void configure() throws EucalyptusCloudException {
		synchronized(ObjectStorageGateway.class) {			
			if(ospClient == null) {
				try {
					ospClient = ObjectStorageProviders.getInstance();
				} catch (Exception ex) {
					LOG.error ("Error getting the configured providerclient for ObjectStorageGateway. Cannot continue", ex);
                    throw new EucalyptusCloudException(ex);
				}
			}
		}

		try {
			ospClient.initialize();
		} catch(S3Exception ex) {
			LOG.error("Error initializing Object Storage Gateway", ex);
			SystemUtil.shutdownWithError(ex.getMessage());
		}

		//Disable torrents
		//Tracker.initialize();
		try {
			if (ospClient != null) {
				//TODO: zhill - this seems wrong in check(), should be in enable() ?				
				ospClient.start();
			}
		} catch(S3Exception ex) {
			LOG.error("Error starting storage backend: " + ex);
		}		
	}

	public static void enable() throws EucalyptusCloudException {
		LOG.debug("Enabling ObjectStorageGateway");
		ospClient.enable();
        LOG.debug("Enabling ObjectStorageGateway complete");
    }

    public static void disable() throws EucalyptusCloudException {
        LOG.debug("Disabling ObjectStorageGateway");
        ospClient.disable();
        LOG.debug("Disabling ObjectStorageGateway complete");
    }

	public static void check() throws EucalyptusCloudException {
		LOG.trace("Checking ObjectStorageGateway");
		ospClient.check();
		LOG.trace("Checking ObjectStorageGateway complete");
	}

	public static void stop() throws EucalyptusCloudException {
		LOG.debug("Checking ObjectStorageGateway preconditions");
		ospClient.stop();
		synchronized(ObjectStorageGateway.class) {
			ospClient = null;
		}
		Tracker.die();

		try {
			ObjectMetadataManagers.getInstance().stop();
		} catch(Exception e) {
			LOG.error("Error stopping object manager",e);
		}

		try {
			BucketMetadataManagers.getInstance().stop();
		} catch(Exception e) {
			LOG.error("Error stopping bucket manager",e);
		}

		LOG.debug("Checking ObjectStorageGateway preconditions");
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#UpdateObjectStorageConfiguration(com.eucalyptus.objectstorage.msgs.UpdateObjectStorageConfigurationType)
	 */
	@Override
	public UpdateObjectStorageConfigurationResponseType updateObjectStorageConfiguration(UpdateObjectStorageConfigurationType request) throws EucalyptusCloudException {
		UpdateObjectStorageConfigurationResponseType reply = request.getReply();
		if(ComponentIds.lookup(Eucalyptus.class).name( ).equals(request.getEffectiveUserId()))
			throw new AccessDeniedException("Only admin can change object storage properties.");
		if(request.getProperties() != null) {
			for(ComponentProperty prop : request.getProperties()) {
				try {
					ConfigurableProperty entry = PropertyDirectory.getPropertyEntry(prop.getQualifiedName());
					//type parser will correctly covert the value
					entry.setValue(prop.getValue());
				} catch (IllegalAccessException e) {
					LOG.error(e, e);
				}
			}
		}
        ospClient.check();
		return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetObjectStorageConfiguration(com.eucalyptus.objectstorage.msgs.GetObjectStorageConfigurationType)
	 */
	@Override
	public GetObjectStorageConfigurationResponseType getObjectStorageConfiguration(GetObjectStorageConfigurationType request) throws EucalyptusCloudException {
		GetObjectStorageConfigurationResponseType reply = request.getReply();
		ConfigurableClass configurableClass = ObjectStorageGlobalConfiguration.class.getAnnotation(ConfigurableClass.class);
		if(configurableClass != null) {
			String prefix = configurableClass.root();
			reply.setProperties((ArrayList<ComponentProperty>) PropertyDirectory.getComponentPropertySet(prefix));
		}
		return reply;
	}

    /**
     * Validity checks based on S3 naming. See http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html
     * Check that the bucket is a valid DNS name (or optionally can look like an IP)
     */
    protected static boolean checkBucketNameValidity(String bucketName) {
        return BucketNameValidatorRepo.getBucketNameValidator().check(bucketName);
    }

	@Override
	public PutObjectResponseType putObject(final PutObjectType request) throws S3Exception {
		logRequest(request);

        try {
            User requestUser = getRequestUser(request);
            Bucket bucket;
            try {
                bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
            } catch(NoSuchEntityException e) {
                LOG.debug("CorrelationId: " + Contexts.lookup().getCorrelationId() + "Responding to client with 404, no bucket found");
                throw new NoSuchBucketException(request.getBucket());
            }


            //TODO: this should be done in binding.
            if(Strings.isNullOrEmpty(request.getContentLength())) {
                //Content-Length is required by S3-spec.
                throw new MissingContentLengthException(request.getBucket() + "/" + request.getKey());
            }

            long objectSize;
            try {
                objectSize = Long.parseLong(request.getContentLength());
            } catch(Exception e) {
                LOG.error("Could not parse content length into a long: " + request.getContentLength(), e);
                throw new MissingContentLengthException(request.getBucket() + "/" + request.getKey());
            }

            ObjectEntity objectEntity = ObjectEntity.newInitializedForCreate(bucket,
                    request.getKey(),
                    objectSize,
                    requestUser);

            if(!OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, objectEntity, objectSize)) {
                throw new AccessDeniedException(request.getBucket());
            }

            //Auth checks passed, check if 100-continue needs to be sent
            if (request.getExpectHeader()) {
                OSGChannelWriter.writeResponse(Contexts.lookup(request.getCorrelationId()), OSGMessageResponse.Continue);
            }
            //Construct and set the ACP properly, post Auth check so no self-auth can occur even accidentally
            AccessControlPolicy acp = getFullAcp(request.getAccessControlList(), requestUser, bucket.getOwnerCanonicalId());
            objectEntity.setAcl(acp);

            final String fullObjectKey = objectEntity.getObjectUuid();
            request.setKey(fullObjectKey); //Ensure the backend uses the new full object name
            try {
            	objectEntity = OsgObjectFactory.getFactory().createObject(ospClient, objectEntity, request.getData(), request.getMetaData(), requestUser);
            } catch(Exception e) {
            	// Wrap the error from back-end with a 500 error
            	throw new InternalErrorException(request.getKey(), e);
            }

            PutObjectResponseType response = request.getReply();
            if(!ObjectStorageProperties.NULL_VERSION_ID.equals(objectEntity.getVersionId())) {
                response.setVersionId(objectEntity.getVersionId());
            }
            response.setEtag(objectEntity.geteTag());
            response.setLastModified(objectEntity.getObjectModifiedTimestamp());
            return response;
        } catch(S3Exception e) {
            LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with: ", e);
            throw e;
        } catch(Exception e) {
            LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
            throw new InternalErrorException(request.getKey(), e);
        }
	}

    /**
     * Gets the user for the request. Uses one in the request if found, if not, uses the Context.
     * If the context is a System context, the system admin (eucalyptus/admin) is returned.
     * @param request
     * @return
     * @throws AccountProblemException
     */
    private User getRequestUser(ObjectStorageRequestType request) throws AccountProblemException {
        try {
            String requestUserId = request.getEffectiveUserId();
            if(Strings.isNullOrEmpty(requestUserId)) {
                return Contexts.lookup().getUser();
            } else {
                if(Principals.systemFullName().getUserId().equals(requestUserId)) {
                    return Accounts.lookupSystemAdmin();
                } else {
                    return Accounts.lookupUserById(requestUserId);
                }
            }
        } catch(AuthException e) {
            throw new AccountProblemException(request.getEffectiveUserId());
        }
    }

    /**
	 * A terse request logging function to log request entry at INFO level.
	 * @param request
	 */
	protected static <I extends ObjectStorageRequestType> void logRequest(I request) {
		StringBuilder canonicalLogEntry = new StringBuilder("osg handling request:" );
		try {			
			String accnt = null;
			String src = null;
			try {
				Context ctx = Contexts.lookup(request.getCorrelationId());
				accnt = ctx.getAccount().getAccountNumber();
				src = ctx.getRemoteAddress().getHostAddress();
			} catch(Exception e) {
				LOG.warn("Failed context lookup by correlation Id: " + request.getCorrelationId());
			} finally {
				if(Strings.isNullOrEmpty(accnt)) {
					accnt = "unknown";
				}
				if(Strings.isNullOrEmpty(src)) {
					src = "unknown";
				}
			}

			canonicalLogEntry.append(" CorrelationId: " + request.getCorrelationId());
			canonicalLogEntry.append(" Operation: " + request.getClass().getSimpleName());
			canonicalLogEntry.append(" Account: " + accnt);
			canonicalLogEntry.append(" Src Ip: " + src);		
			canonicalLogEntry.append(" Bucket: " + request.getBucket());
			canonicalLogEntry.append(" Object: " + request.getKey());
			if(request instanceof GetObjectType) {
				canonicalLogEntry.append(" VersionId: " + ((GetObjectType)request).getVersionId());
			} else if(request instanceof PutObjectType) {
				canonicalLogEntry.append(" ContentMD5: " + ((PutObjectType)request).getContentMD5());
			}		
			LOG.info(canonicalLogEntry.toString());
		} catch(Exception e) {
			LOG.warn("Problem formatting request log entry. Incomplete entry: " + canonicalLogEntry.toString(), e);
		}		
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#HeadBucket(com.eucalyptus.objectstorage.msgs.HeadBucketType)
	 */
	@Override
	public HeadBucketResponseType headBucket(HeadBucketType request) throws S3Exception {
		Bucket bucket = getBucketAndCheckAuthorization(request);

        HeadBucketResponseType reply = request.getReply();
        reply.setBucket(bucket.getBucketName());
        reply.setStatus(HttpResponseStatus.OK);
        reply.setStatusMessage("OK");
        reply.setTimestamp(new Date());
        return reply;
	}

    /**
     * Create a full ACP object from a user and an ACL object. Expands canned-acls and
     * adds owner information
     * @param acl
     * @param requestUser
     * @return
     * @throws Exception
     */
    protected AccessControlPolicy getFullAcp(@Nonnull AccessControlList acl, @Nonnull User requestUser, @Nullable String extantBucketOwnerCanonicalId) throws Exception {
        //Generate a full ACP based on the request. If empty or null acl, generates a 'private' acl with fullcontrol for owner
        AccessControlPolicy tmpPolicy = new AccessControlPolicy();
        tmpPolicy.setAccessControlList(acl);
        if(extantBucketOwnerCanonicalId == null) {
            return AclUtils.processNewResourcePolicy(requestUser, tmpPolicy , null);
        } else {
            return AclUtils.processNewResourcePolicy(requestUser, tmpPolicy , extantBucketOwnerCanonicalId);
        }
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#CreateBucket(com.eucalyptus.objectstorage.msgs.CreateBucketType)
	 */
	@Override
	public CreateBucketResponseType createBucket(final CreateBucketType request) throws S3Exception {
		logRequest(request);
        try {
            User requestUser = getRequestUser(request);
            Account requestAccount = requestUser.getAccount();
            long bucketCount;

            //Check the validity of the bucket name.
            if (!checkBucketNameValidity(request.getBucket())) {
                throw new InvalidBucketNameException(request.getBucket());
            }

            final AccessControlPolicy acPolicy = getFullAcp(request.getAccessControlList(), requestUser, null);
            Bucket bucket = Bucket.getInitializedBucket(request.getBucket(), requestUser.getUserId(), acPolicy, request.getLocationConstraint());

            if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, null, 1)) {
                /*
				 * This is a secondary check, independent to the iam quota check, based on the configured max bucket count property.
				 */
                if (!Contexts.lookup().hasAdministrativePrivileges() &&
                        BucketMetadataManagers.getInstance().countBucketsByAccount(requestAccount.getCanonicalId()) >= ObjectStorageGlobalConfiguration.max_buckets_per_account) {
                    throw new TooManyBucketsException(request.getBucket());
                }

                try {
                    //Do the actual creation
                    bucket = OsgBucketFactory.getFactory().createBucket(ospClient,
                            bucket,
                            request.getCorrelationId(),
                            requestUser);

                    CreateBucketResponseType reply = request.getReply();
                    reply.setStatus(HttpResponseStatus.OK);
                    reply.setBucket(bucket.getBucketName());
                    reply.setTimestamp(new Date());
                    reply.setStatusMessage("OK");
                    LOG.info("CorrelationId: " + request.getCorrelationId() + " Responding with " + reply.getStatus().toString());
                    return reply;
                } catch(BucketAlreadyExistsException e) {
                    Bucket extantBucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
                    if(extantBucket.isOwnedBy(requestAccount.getCanonicalId())) {
                        /*//Update the bucket metadata if the bucket already exists...ACL specifically. only for owner or any user with write_acp?
                        if(!extantBucket.getAccessControlPolicy().equals(acPolicy)) {
                            //Try to update the ACL
                            SetBucketAccessControlPolicyType aclRequest = new SetBucketAccessControlPolicyType();
                            aclRequest.setUser(request.getUser());
                            aclRequest.setAccessControlPolicy(acPolicy);
                            aclRequest.setBucket(request.getBucket());
                            try {
                                SetBucketAccessControlPolicyResponseType response = setRESTBucketAccessControlPolicy(aclRequest);
                            } catch(S3Exception s3ex) {

                            } catch(Exception aclEx) {

                            }
                        } else {
                            //All the same, do nothing.
                        }
                        */

                        //reply ok, bucket already exists for this owner
                        CreateBucketResponseType reply = request.getReply();
                        reply.setStatus(HttpResponseStatus.OK);
                        reply.setBucket(bucket.getBucketName());
                        reply.setStatusMessage("OK");
                        LOG.info("CorrelationId: " + request.getCorrelationId() + " Responding with " + reply.getStatus().toString());
                        return reply;
                    } else {
                        // Wrap the error from back-end with a 500 error
                    	throw new InternalErrorException(request.getBucket(), e);
                    }
                }
            } else {
                LOG.error("CorrelationId: " + request.getCorrelationId() + " Create bucket " + request.getBucket() + " access is denied based on acl and/or IAM policy");
                throw new AccessDeniedException(request.getBucket());
            }
        } catch(Exception ex) {
            if(ex instanceof S3Exception) {
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with: ", ex);
                throw (S3Exception)ex;
            } else {
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", ex);
                throw new InternalErrorException(request.getBucket(), ex);
            }
        }
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#DeleteBucket(com.eucalyptus.objectstorage.msgs.DeleteBucketType)
	 */
	@Override
	public DeleteBucketResponseType deleteBucket(final DeleteBucketType request) throws S3Exception {
		Bucket bucket;
        try {
            bucket = getBucketAndCheckAuthorization(request);
        } catch(NoSuchBucketException e) {
            //This is okay, fall through
            bucket = null;
        }

        if(bucket != null) {
            try {
                if(ObjectMetadataManagers.getInstance().countValid(bucket) > 0) {
                    throw new BucketNotEmptyException(bucket.getBucketName());
                }
            } catch(S3Exception e) {
                throw e;
            } catch(Exception e) {
                LOG.warn("Problem with bucket deletion during emptiness checks", e);
                throw new InternalErrorException(e);
            }
            
            try {
            	OsgBucketFactory.getFactory().deleteBucket(ospClient, bucket, request.getCorrelationId(), Contexts.lookup().getUser());
            } catch (Exception e) {
            	// Wrap the error from back-end with a 500 error
                LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
                throw new InternalErrorException(request.getKey(), e);
            }
        }

        //Return success even if no deletion was needed. This is per s3-spec.
        DeleteBucketResponseType reply = request.getReply();
        reply.setStatus(HttpResponseStatus.NO_CONTENT);
        reply.setStatusMessage("NoContent");
        LOG.info("CorrelationId: " + request.getCorrelationId() + " Responding with " + reply.getStatus().toString());
        return reply;
    }

	protected static ListAllMyBucketsList generateBucketListing(List<Bucket> buckets) {
		ListAllMyBucketsList bucketList = new ListAllMyBucketsList();
		bucketList.setBuckets(new ArrayList<BucketListEntry>());
		for(Bucket b : buckets ) {
			bucketList.getBuckets().add(b.toBucketListEntry());
		}
		return bucketList;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#ListAllMyBuckets(com.eucalyptus.objectstorage.msgs.ListAllMyBucketsType)
	 */
	@Override
	public ListAllMyBucketsResponseType listAllMyBuckets(ListAllMyBucketsType request) throws S3Exception {
		logRequest(request);

		//Create a fake bucket record just for IAM verification. The IAM policy is only valid for arn:s3:* so empty should match
		/*
		 * ListAllMyBuckets uses a weird authentication check for IAM because it is technically a bucket operation(there are no service operations)
		 * , but the request is not against a specific bucket and the account admin cannot limit listallbuckets output on a per-bucket basis.
		 * The only valid resource to grant s3:ListAllMyBuckets to is '*'.
		 * 
		 * This sets up a fake bucket so that the ACL checks and basic ownership checks can be passed, leaving just the IAM permission
		 * check.
		 */
        Bucket fakeBucket = new Bucket();
        fakeBucket.setBucketName("*"); // '*' should match this, and only this since it isn't a valid bucket name
        fakeBucket.setOwnerCanonicalId(Contexts.lookup().getAccount().getCanonicalId()); // make requestor the owner of fake bucket
        request.setBucket(fakeBucket.getBucketName());

		if(OsgAuthorizationHandler.getInstance().operationAllowed(request, fakeBucket, null, 0)) {
			ListAllMyBucketsResponseType response = request.getReply();
			/*
			 * This is a strictly metadata operation, no backend is hit. The sync of metadata in OSG to backend is done elsewhere asynchronously.
			 */
			Account accnt;
			try {
				accnt = Contexts.lookup().getAccount();
				if(accnt == null) {
					throw new NoSuchContextException();
				}
			} catch (NoSuchContextException e) {
				try {
					accnt = Accounts.lookupUserByAccessKeyId(request.getAccessKeyID()).getAccount();
				} catch(AuthException ex) {
					LOG.error("Could not retrieve canonicalId for user with accessKey: " + request.getAccessKeyID());
					throw new InternalErrorException();
				}
			}
			try {
				List<Bucket> listing = BucketMetadataManagers.getInstance().lookupBucketsByOwner(accnt.getCanonicalId());
				response.setBucketList(generateBucketListing(listing));
				response.setOwner(AclUtils.buildCanonicalUser(accnt));
				return response;
			} catch(Exception e) {
				throw new InternalErrorException();
			}
		} else {
			AccessDeniedException ex = new AccessDeniedException("ListAllMyBuckets");
			ex.setMessage("Insufficient permissions to list buckets. Check with your account administrator");
			ex.setResourceType("Service");
			throw ex;
		}		
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetBucketAccessControlPolicy(com.eucalyptus.objectstorage.msgs.GetBucketAccessControlPolicyType)
	 */
	@Override
	public GetBucketAccessControlPolicyResponseType getBucketAccessControlPolicy(GetBucketAccessControlPolicyType request) throws S3Exception
	{
		logRequest(request);
		Bucket bucket;
		try {
			bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
		} catch(NoSuchElementException e) {
			throw new NoSuchBucketException(request.getBucket());
		} catch(Exception e) {
			LOG.error("Error getting metadata for object " + request.getBucket() + " " + request.getKey());
			throw new InternalErrorException(request.getBucket() + "/?acl");
		}

		if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, null, 0)) {
			//Get the listing from the back-end and copy results in.
			GetBucketAccessControlPolicyResponseType reply = request.getReply();
			reply.setBucket(request.getBucket());
			try {
				reply.setAccessControlPolicy(bucket.getAccessControlPolicy());
			} catch(Exception e) {
				throw new InternalErrorException(request.getBucket() + "/?acl");
			}
			return reply;
		} else {
			throw new AccessDeniedException(request.getBucket());
		}
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#PostObject(com.eucalyptus.objectstorage.msgs.PostObjectType)
	 */
	@Override
	public PostObjectResponseType postObject (PostObjectType request) throws S3Exception {
        String bucketName = request.getBucket();
        String key = request.getKey();

        PutObjectType putObject = new PutObjectType();
        putObject.setUserId(Contexts.lookup().getUserFullName().getUserId());
        putObject.setBucket(bucketName);
        putObject.setKey(key);
        putObject.setAccessControlList(request.getAccessControlList());
        putObject.setContentType(request.getContentType());
        putObject.setContentLength(request.getContentLength());
        putObject.setAccessKeyID(request.getAccessKeyID());
        putObject.setEffectiveUserId(request.getEffectiveUserId());
        putObject.setCredential(request.getCredential());
        putObject.setIsCompressed(request.getIsCompressed());
        putObject.setMetaData(request.getMetaData());
        putObject.setStorageClass(request.getStorageClass());

        PutObjectResponseType putObjectResponse = putObject(putObject);

        String etag = putObjectResponse.getEtag();
        PostObjectResponseType reply = request.getReply();
        reply.setEtag(etag);
        reply.setLastModified(putObjectResponse.getLastModified());
        reply.set_return(putObjectResponse.get_return());
        reply.setMetaData(putObjectResponse.getMetaData());
        reply.setErrorCode(putObjectResponse.getErrorCode());
        reply.setStatusMessage(putObjectResponse.getStatusMessage());

        String successActionRedirect = request.getSuccessActionRedirect();
        if (successActionRedirect != null) {
            try {
                java.net.URI addrUri = new URL(successActionRedirect).toURI();
                InetAddress.getByName(addrUri.getHost());
            } catch (Exception ex) {
                LOG.warn(ex);
            }
            String paramString = "bucket=" + bucketName + "&key=" + key + "&etag=quot;" + etag + "quot;";
            reply.setRedirectUrl(successActionRedirect + "?" + paramString);
        } else {
            Integer successActionStatus = request.getSuccessActionStatus();
            if (successActionStatus != null) {
                if ((successActionStatus == 200) || (successActionStatus == 201)) {
                    reply.setSuccessCode(successActionStatus);
                    if (successActionStatus == 200) {
                        return reply;
                    } else {
                        reply.setBucket(bucketName);
                        reply.setKey(key);
                        reply.setLocation(Topology.lookup(ObjectStorage.class).getUri().getHost() + "/" + bucketName + "/" + key);
                    }
                } else {
                    reply.setSuccessCode(204);
                    return reply;
                }
            }
        }
        return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#DeleteObject(com.eucalyptus.objectstorage.msgs.DeleteObjectType)
	 */
	@Override
	public DeleteObjectResponseType deleteObject (final DeleteObjectType request) throws S3Exception {
		ObjectEntity objectEntity;
		try {
			objectEntity = getObjectEntityAndCheckPermissions(request, null);
		} catch(NoSuchBucketException | NoSuchKeyException | NoSuchEntityException | NoSuchElementException e) {
			//Nothing to do, object doesn't exist. Return 204 per S3 spec
			DeleteObjectResponseType reply = request.getReply();
			reply.setStatus(HttpResponseStatus.NO_CONTENT);
			reply.setStatusMessage("No Content");
			return reply;
		} catch(Exception e) {
			LOG.error("Error getting bucket metadata for bucket " + request.getBucket());
			throw new InternalErrorException(request.getBucket());
		}

        try {
            OsgObjectFactory.getFactory().logicallyDeleteObject(ospClient, objectEntity, Contexts.lookup().getUser());
            try {
                fireObjectUsageEvent(S3ObjectEvent.S3ObjectAction.OBJECTDELETE, objectEntity.getBucket().getBucketName(),
                        objectEntity.getObjectKey(), objectEntity.getVersionId(),
                        Contexts.lookup().getUser().getUserId(), objectEntity.getSize());
            }
            catch (Exception e) {
            LOG.warn("caught exception while attempting to fire reporting event, exception message - " + e.getMessage());
            }

            DeleteObjectResponseType reply = request.getReply();
            reply.setStatus(HttpResponseStatus.NO_CONTENT);
            reply.setStatusMessage("No Content");
            return reply;
        } catch (Exception e) {
            LOG.error("Transaction error during delete object: " + request.getBucket() + "/" + request.getKey(), e);
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
        }
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#ListBucket(com.eucalyptus.objectstorage.msgs.ListBucketType)
	 */
	@Override
	public ListBucketResponseType listBucket(ListBucketType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        //Get the listing from the back-end and copy results in.
        //return ospClient.listBucket(request);
        ListBucketResponseType reply = request.getReply();
        int maxKeys = 1000;
        try {
            if(!Strings.isNullOrEmpty(request.getMaxKeys())) {
                maxKeys = Integer.parseInt(request.getMaxKeys());
            }
        } catch(NumberFormatException e) {
            LOG.error("Failed to parse maxKeys from request properly: " + request.getMaxKeys(), e);
            throw new InvalidArgumentException("MaxKeys");
        }
        reply.setMaxKeys(maxKeys);
        reply.setName(request.getBucket());
        reply.setDelimiter(request.getDelimiter());
        reply.setMarker(request.getMarker());
        reply.setPrefix(request.getPrefix());
        reply.setIsTruncated(false);

        PaginatedResult<ObjectEntity> result;
        try {
            result = ObjectMetadataManagers.getInstance().listPaginated(bucket, maxKeys, request.getPrefix(), request.getDelimiter(), request.getMarker());
        } catch(Exception e) {
            LOG.error("Error getting object listing for bucket: " + request.getBucket(), e);
            throw new InternalErrorException(request.getBucket());
        }

        if(result != null) {
            reply.setContents(new ArrayList<ListEntry>());

            for(ObjectEntity obj : result.getEntityList()){
                reply.getContents().add(obj.toListEntry());
            }

            if(result.getCommonPrefixes() != null && result.getCommonPrefixes().size() > 0) {
                reply.setCommonPrefixesList(new ArrayList<CommonPrefixesEntry>());

                for(String s : result.getCommonPrefixes()) {
                    reply.getCommonPrefixesList().add(new CommonPrefixesEntry(s));
                }
            }
            reply.setIsTruncated(result.isTruncated);
            if(result.isTruncated) {
                if(	result.getLastEntry() instanceof ObjectEntity) {
                    reply.setNextMarker(((ObjectEntity)result.getLastEntry()).getObjectKey());
                } else {
                    //If max-keys = 0, then last entry may be empty
                    reply.setNextMarker((result.getLastEntry() != null ? result.getLastEntry().toString() : ""));
                }
            }
        } else {
            //Do nothing
            //				reply.setContents(new ArrayList<ListEntry>());
        }

        return reply;
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetObjectAccessControlPolicy(com.eucalyptus.objectstorage.msgs.GetObjectAccessControlPolicyType)
	 */
	@Override
	public GetObjectAccessControlPolicyResponseType getObjectAccessControlPolicy(GetObjectAccessControlPolicyType request) throws S3Exception {
		ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, request.getVersionId());

        //Get the listing from the back-end and copy results in.
        GetObjectAccessControlPolicyResponseType reply = request.getReply();
        reply.setBucket(request.getBucket());
        try {
            reply.setAccessControlPolicy(objectEntity.getAccessControlPolicy());
        } catch(Exception e) {
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
        }
        return reply;
	}

    private static boolean isCannedAclPolicy(AccessControlPolicy acp) {
        if(acp.getOwner() == null && acp.getAccessControlList() != null && acp.getAccessControlList().getGrants().size() == 1) {
            try {
                return ObjectStorageProperties.CannedACL.valueOf(acp.getAccessControlList().getGrants().get(0).getPermission().replace("-", "_")) != null;
            } catch(IllegalArgumentException e) {
                return false;
            }
        } else {
            //canned-acls are not bound with owner imbedded
            return false;
        }
    }
	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#SetRESTBucketAccessControlPolicy(com.eucalyptus.objectstorage.msgs.SetBucketAccessControlPolicyType)
	 */
	@Override
	public SetBucketAccessControlPolicyResponseType setBucketAccessControlPolicy(final SetBucketAccessControlPolicyType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);
        if(request.getAccessControlPolicy() == null || request.getAccessControlPolicy().getAccessControlList() == null) {
            //Can't set to null
            throw new MalformedACLErrorException(request.getBucket() + "/" + request.getKey() + "?acl");
        } else {
            AccessControlPolicy fullPolicy;
            //Expand the acl first
            if(isCannedAclPolicy(request.getAccessControlPolicy())) {
                fullPolicy = new AccessControlPolicy();
                fullPolicy.setOwner(new CanonicalUser(bucket.getOwnerCanonicalId(), bucket.getOwnerDisplayName()));
                try {
                    fullPolicy.setAccessControlList(AclUtils.expandCannedAcl(request.getAccessControlPolicy().getAccessControlList(), bucket.getOwnerCanonicalId(), null));
                } catch(Exception e) {
                    throw new MalformedACLErrorException(request.getBucket() + "?acl");
                }
            } else {
                fullPolicy = request.getAccessControlPolicy();
                //Must have explicit owner
                if(fullPolicy.getAccessControlList() == null || fullPolicy.getOwner() == null) {
                    //Something happened in acl expansion.
                    LOG.error("Cannot put ACL that does not exist in request");
                    throw new MalformedACLErrorException(request.getBucket() + "?acl");
                }
            }

            try {
                BucketMetadataManagers.getInstance().setAcp(bucket, fullPolicy);
                SetBucketAccessControlPolicyResponseType reply = request.getReply();
                reply.setBucket(request.getBucket());
                return reply;
            } catch(Exception e) {
                LOG.error("Transaction error updating bucket ACL for bucket " + request.getBucket(),e);
                throw new InternalErrorException(request.getBucket() + "?acl");
            }
        }
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#SetRESTObjectAccessControlPolicy(com.eucalyptus.objectstorage.msgs.SetObjectAccessControlPolicyType)
	 */
	@Override
    public SetObjectAccessControlPolicyResponseType setObjectAccessControlPolicy(final SetObjectAccessControlPolicyType request) throws S3Exception {
        ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, request.getVersionId());

        SetObjectAccessControlPolicyResponseType reply = request.getReply();
        final String bucketOwnerId = objectEntity.getBucket().getOwnerCanonicalId();
        final String objectOwnerId = objectEntity.getOwnerCanonicalId();
        try {
            String aclString = null;
            if(request.getAccessControlPolicy() == null || request.getAccessControlPolicy().getAccessControlList() == null) {
                //Can't set to null
                throw new MalformedACLErrorException(request.getBucket() + "/" + request.getKey() + "?acl");
            } else {
                //Expand the acl first
                request.getAccessControlPolicy().setAccessControlList(AclUtils.expandCannedAcl(request.getAccessControlPolicy().getAccessControlList(), bucketOwnerId, objectOwnerId));
                if(request.getAccessControlPolicy() == null || request.getAccessControlPolicy().getAccessControlList() == null) {
                    //Something happened in acl expansion.
                    LOG.error("Cannot put ACL that does not exist in request");
                    throw new InternalErrorException(request.getBucket() + "/" + request.getKey() + "?acl");
                } else {
                    //Add in the owner entry if not present
                    if(request.getAccessControlPolicy().getOwner() == null ) {
                        request.getAccessControlPolicy().setOwner(new CanonicalUser(objectOwnerId, objectEntity.getOwnerDisplayName()));
                    }
                }

                //Marshal into a string
                aclString = S3AccessControlledEntity.marshallAcpToString(request.getAccessControlPolicy());
                if(Strings.isNullOrEmpty(aclString)) {
                    throw new MalformedACLErrorException(request.getBucket() + "/" + request.getKey() + "?acl");
                }
            }

            //Get the listing from the back-end and copy results in.
            ObjectMetadataManagers.getInstance().setAcp(objectEntity, request.getAccessControlPolicy());
            return reply;
        } catch(Exception e) {
            LOG.error("Internal error during PUT object?acl for object " + request.getBucket() + "/" + request.getKey(), e);
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
        }
    }

	/**
	 * Common lookupBucket routine used by simple and extended GETs.
	 */
	protected DefaultHttpResponse createHttpResponse(ObjectStorageDataGetResponseType reply) {
		DefaultHttpResponse httpResponse = new DefaultHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
		long contentLength = reply.getSize();
		String contentType = reply.getContentType();
		String etag = reply.getEtag();
		Date lastModified = reply.getLastModified();
		String contentDisposition = reply.getContentDisposition();
		httpResponse.addHeader( HttpHeaders.Names.CONTENT_TYPE, contentType != null ? contentType : "binary/octet-stream" );
		if(etag != null)
			httpResponse.addHeader(HttpHeaders.Names.ETAG, "\"" + etag + "\""); //etag in quotes, per s3-spec.
		httpResponse.addHeader(HttpHeaders.Names.LAST_MODIFIED, DateFormatter.dateToHeaderFormattedString(lastModified));
		if(contentDisposition != null) {
			httpResponse.addHeader("Content-Disposition", contentDisposition);
		}
		httpResponse.addHeader( HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(contentLength));
		String versionId = reply.getVersionId();
		if(versionId != null) {
			httpResponse.addHeader(ObjectStorageProperties.X_AMZ_VERSION_ID, versionId);
		}
		httpResponse.setHeader(HttpHeaders.Names.DATE, DateFormatter.dateToHeaderFormattedString(new Date()));
		
		//write extra headers
		if(reply.getByteRangeEnd() != null) {
			httpResponse.addHeader("Content-Range", reply.getByteRangeStart() + "-" + reply.getByteRangeEnd() + "/" + reply.getSize());
		}
		return httpResponse;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetObject(com.eucalyptus.objectstorage.msgs.GetObjectType)
	 */
	@Override
	public GetObjectResponseType getObject(final GetObjectType request) throws S3Exception {
        ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, request.getVersionId());
        //Handle 100-continue here.
        if(objectEntity.getIsDeleteMarker()) {
            throw new NoSuchKeyException(request.getKey());
        }

        request.setKey(objectEntity.getObjectUuid());
        request.setBucket(objectEntity.getBucket().getBucketUuid());
        GetObjectResponseType reply;
        final String originalVersionId = request.getVersionId();
        //Versioning not used on backend
        request.setVersionId(null);
        try {
             reply = ospClient.getObject(request);
        } catch(Exception e) {
        	// Wrap the error from back-end with a 500 error
            LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
            throw new InternalErrorException(objectEntity.getResourceFullName(), e);
        }

        reply.setLastModified(objectEntity.getObjectModifiedTimestamp());
        reply.setEtag(objectEntity.geteTag());
        reply.setVersionId(objectEntity.getVersionId());
        reply.setHasStreamingData(true);

        if(request.getInlineData()) {
            //Write the data into a string and include in response. Only use for small internal operations.
            //Cannot be invoked by S3 clients (inline flag is not part of s3 binding)
            if(reply.getSize() * 4 > ObjectStorageProperties.MAX_INLINE_DATA_SIZE) {
                LOG.error("Base64 encoded object size: "+ reply.getSize() + " exceeds maximum inline response size: " + ObjectStorageProperties.MAX_INLINE_DATA_SIZE + "bytes. Cannot return response.");
                throw new InlineDataTooLargeException(request.getBucket() + "/" + request.getKey());
            }

            byte[] buffer = new byte[ObjectStorageProperties.IO_CHUNK_SIZE];
            int readLength;
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try {
                while((readLength = reply.getDataInputStream().read(buffer)) >= 0) {
                    data.write(buffer, 0, readLength);
                }
                reply.setBase64Data(B64.url.encString(data.toByteArray()));
            } catch(BufferOverflowException e) {
                LOG.error("Maximum inline response size: " + ObjectStorageProperties.MAX_INLINE_DATA_SIZE + "bytes exceeded. Cannot return response.",e);
                throw new InlineDataTooLargeException(request.getBucket() + "/" + request.getKey());
            } catch(IOException e) {
                LOG.error("Error reading data to write into in-line response",e);
                throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
            } finally {
                try {
                    reply.getDataInputStream().close();
                } catch(IOException ex) {
                    LOG.error("Could not close inputstream for data content on inline-data GetObject.",ex);
                }
                reply.setDataInputStream(null); //null out the input stream as it is no longer valid
                reply.setHasStreamingData(false);
            }
            //return reply;
        }

        return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetObjectExtended(com.eucalyptus.objectstorage.msgs.GetObjectExtendedType)
	 */
	@Override
	public GetObjectExtendedResponseType getObjectExtended(GetObjectExtendedType request) throws S3Exception {
        ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, null);
        request.setKey(objectEntity.getObjectUuid());
        request.setBucket(objectEntity.getBucket().getBucketUuid());
        try {
            request.setBucket(objectEntity.getBucket().getBucketUuid());
            request.setKey(objectEntity.getObjectUuid());

            GetObjectExtendedResponseType response = ospClient.getObjectExtended(request);

            response.setVersionId(objectEntity.getVersionId());
            response.setLastModified(objectEntity.getObjectModifiedTimestamp());
            return response;
        } catch(Exception e) {
        	// Wrap the error from back-end with a 500 error
            LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey(), e);
        }
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetObject(com.eucalyptus.objectstorage.msgs.GetObjectType)
	 */
	@Override
	public HeadObjectResponseType headObject(HeadObjectType request) throws S3Exception {
        ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, request.getVersionId());

        if(objectEntity.getIsDeleteMarker()) {
            throw new NoSuchKeyException(request.getKey());
        }

        HeadObjectResponseType reply = request.getReply();
        request.setKey(objectEntity.getObjectUuid());
        request.setBucket(objectEntity.getBucket().getBucketUuid());
        final String originalVersionId = request.getVersionId();
        try {
            //Unset the versionId because it isn't used on backend
            request.setVersionId(null);
            HeadObjectResponseType backendReply = ospClient.headObject(request);
            reply.setMetaData(backendReply.getMetaData());
        } catch(S3Exception e) {
        	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
            //We don't dispatch unless it exists and should be available. An error from the backend would be confusing. This is an internal issue.
            throw new InternalErrorException(e);
        }

        reply.setLastModified(objectEntity.getObjectModifiedTimestamp());
        reply.setSize(objectEntity.getSize());
        reply.setVersionId(objectEntity.getVersionId());
        reply.setEtag(objectEntity.geteTag());
        reply.setVersionId(objectEntity.getVersionId());

        return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetBucketLocation(com.eucalyptus.objectstorage.msgs.GetBucketLocationType)
	 */
	@Override
	public GetBucketLocationResponseType getBucketLocation(GetBucketLocationType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        GetBucketLocationResponseType reply = request.getReply();
        reply.setLocationConstraint(bucket.getLocation() == null ? "" : bucket.getLocation());
        reply.setBucket(request.getBucket());
        return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#CopyObject(com.eucalyptus.objectstorage.msgs.CopyObjectType)
	 */
	@Override
	public CopyObjectResponseType copyObject(CopyObjectType request) throws S3Exception {
        logRequest(request);
        User requestUser = Contexts.lookup().getUser();
        final Bucket srcBucket = ensureBucketExists(request.getSourceBucket());

        final ObjectEntity srcObject;
        try {
            srcObject = ObjectMetadataManagers.getInstance().lookupObject(srcBucket, request.getSourceObject(), request.getSourceVersionId());
        } catch(NoSuchElementException e) {
            throw new NoSuchKeyException(request.getSourceBucket() + "/" + request.getSourceObject());
        } catch(Exception e) {
            throw new InternalErrorException(request.getSourceBucket());
        }

        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, srcBucket, srcObject, 0)) {
            CopyObjectResponseType reply = request.getReply();

            Bucket destBucket = ensureBucketExists(request.getDestinationBucket());

            String versionId = destBucket.getVersion().toString();
            /* try {
                versionId = BucketManagers.getInstance().getVersionId(destBucket);
            } catch (Exception e2) {
                LOG.error("Error generating version Id string by bucket " + destBucket.getBucketName(), e2);
                throw new InternalErrorException(destBucket.getBucketName() + "/" + request.getDestinationObject());
            }*/

            String destinationKey = request.getDestinationObject();
            String metadataDirective = request.getMetadataDirective();
            String copyIfMatch = request.getCopySourceIfMatch();
            String copyIfNoneMatch = request.getCopySourceIfNoneMatch();
            Date copyIfUnmodifiedSince = request.getCopySourceIfUnmodifiedSince();
            Date copyIfModifiedSince = request.getCopySourceIfModifiedSince();

            if (metadataDirective == null || "".equals(metadataDirective)) {
                metadataDirective = "COPY";
            }

            long newBucketSize = ( destBucket.getBucketSize() == null ? 0l : destBucket.getBucketSize().longValue() )
                    + srcObject.getSize().longValue();

            ObjectEntity destObject = null;
            Long origDestObjectSize = null; // used in reporting event
            try {
                destObject = ObjectMetadataManagers.getInstance().lookupObject(destBucket, destinationKey, null);
                origDestObjectSize = destObject.getSize();
            } catch (NoSuchElementException nse) {
                // is okay, creating a new ObjectEntity
            } catch (Exception e) {
                LOG.error("exception occurred while checking if destination object in bucket "
                        + destBucket.getBucketName() + " with key " + destinationKey + " already exists", e);
                throw new InternalErrorException(destBucket.getBucketName() + "/" + destinationKey);
            }

                try {
                    if (destObject == null) {
                        destObject = ObjectEntity.newInitializedForCreate(destBucket, destinationKey,
                            srcObject.getSize().longValue(), requestUser);
                    }
                } catch (Exception e) {
                    LOG.error("Error intializing entity for persiting object metadata for " + destBucket.getBucketName()
                            + "/" + request.getDestinationObject());
                    throw new InternalErrorException(destBucket.getBucketName() + "/" + destinationKey);
                }


            if(OsgAuthorizationHandler.getInstance().operationAllowed(request, destBucket, destObject, newBucketSize)) {
                if (copyIfMatch != null) {
                    if (!copyIfMatch.equals(srcObject.geteTag())) {
                        throw new PreconditionFailedException(srcObject.getObjectKey() + " CopySourceIfMatch: " + copyIfMatch);
                    }
                }
                if (copyIfNoneMatch != null) {
                    if (copyIfNoneMatch.equals(srcObject.geteTag())) {
                        throw new PreconditionFailedException(srcObject.getObjectKey() + " CopySourceIfNoneMatch: " + copyIfNoneMatch);
                    }
                }
                if (copyIfUnmodifiedSince != null) {
                    long unmodifiedTime = copyIfUnmodifiedSince.getTime();
                    long objectTime = srcObject.getObjectModifiedTimestamp().getTime();
                    if (unmodifiedTime < objectTime) {
                        throw new PreconditionFailedException(srcObject.getObjectKey() + " CopySourceIfUnmodifiedSince: " + copyIfUnmodifiedSince.toString());
                    }
                }
                if (copyIfModifiedSince != null) {
                    long modifiedTime = copyIfModifiedSince.getTime();
                    long objectTime = srcObject.getObjectModifiedTimestamp().getTime();
                    if (modifiedTime > objectTime) {
                        throw new PreconditionFailedException(srcObject.getObjectKey() + " CopySourceIfModifiedSince: " + copyIfModifiedSince.toString());
                    }
                }

                try {
                    AccessControlPolicy acp = getFullAcp(request.getAccessControlList(), requestUser, destBucket.getOwnerCanonicalId());
                    destObject.setAcl(acp);
                } catch (Exception e) {
                    LOG.warn("encountered an exception while constructing access control policy to set on "
                            + destBucket.getBucketName() + "/" + destObject.getObjectKey(), e);
                    throw new InternalErrorException(destBucket.getBucketName() + "/"
                            + destObject.getObjectKey() + "?acl");
                }

                destObject.setSize(srcObject.getSize());
                destObject.setStorageClass(srcObject.getStorageClass());
                destObject.setOwnerCanonicalId(srcObject.getOwnerCanonicalId());
                destObject.setOwnerDisplayName(srcObject.getOwnerDisplayName());
                destObject.setOwnerIamUserId(srcObject.getOwnerIamUserId());
                destObject.setOwnerIamUserDisplayName(srcObject.getOwnerIamUserDisplayName());
                String etag = srcObject.geteTag();
                final Date srcLastMod = srcObject.getObjectModifiedTimestamp();
                destObject.seteTag(etag);
                destObject.setObjectModifiedTimestamp(srcLastMod);
                destObject.setIsLatest(Boolean.TRUE);

                if (destBucket.getVersioning() == ObjectStorageProperties.VersioningStatus.Enabled ) {
                    reply.setCopySourceVersionId(versionId);
                    reply.setVersionId(versionId);
                }

                String sourceObjUuid = srcObject.getObjectUuid();
                String sourceBckUuid = srcBucket.getBucketUuid();
                String destObjUuid = destObject.getObjectUuid();
                String destBckUuid = destBucket.getBucketUuid();
                request.setSourceObject(sourceObjUuid);
                request.setSourceBucket(sourceBckUuid);
                request.setSourceVersionId(ObjectStorageProperties.NULL_VERSION_ID);
                request.setDestinationObject(destObjUuid);
                request.setDestinationBucket(destBckUuid);
                try {
                    ObjectEntity objectEntity = OsgObjectFactory.getFactory().copyObject(ospClient, destObject, request, requestUser, metadataDirective);
                    reply.setLastModified(DateFormatter.dateToListingFormattedString(objectEntity.getObjectModifiedTimestamp()));
                    reply.setEtag(objectEntity.geteTag());
                    try {
                        fireObjectCreationEvent(destBucket.getBucketName(), destObject.getObjectKey(), versionId,
                                requestUser.getUserId(), destObject.getSize(), origDestObjectSize);
                    } catch (Exception ex) {
                        LOG.debug("Failed to fire reporting event for OSG COPY object operation", ex);
                    }
                }
                catch (Exception ex) {
                	// Wrap the error from back-end with a 500 error
                	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", ex);
                    throw new InternalErrorException("Could not copy " + srcBucket.getBucketName() +
                            "/" + srcObject.getObjectKey() + " to " + destBucket.getBucketName() +
                            "/" + destObject.getObjectKey(), ex);
                }
                request.setSourceObject(srcObject.getObjectKey());
                request.setSourceVersionId(srcObject.getVersionId());
                request.setDestinationObject(destinationKey);
                request.setSourceBucket(srcBucket.getBucketName());
                request.setDestinationBucket(destBucket.getBucketName());
                return reply;
            }
            else {
                throw new AccessDeniedException(destBucket.getBucketName() + "/" + destinationKey);
            }
        } else {
            throw new AccessDeniedException(srcBucket.getBucketName());
        }

	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetBucketLoggingStatus(com.eucalyptus.objectstorage.msgs.GetBucketLoggingStatusType)
	 */
	@Override
	public GetBucketLoggingStatusResponseType getBucketLoggingStatus(GetBucketLoggingStatusType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        GetBucketLoggingStatusResponseType reply = request.getReply();
        LoggingEnabled loggingConfig = new LoggingEnabled();
        if(bucket.getLoggingEnabled()) {
            TargetGrants grants = new TargetGrants();
            try {
                Bucket targetBucket = BucketMetadataManagers.getInstance().lookupExtantBucket(bucket.getTargetBucket());
                grants.setGrants(targetBucket.getAccessControlPolicy().getAccessControlList().getGrants());
            } catch(Exception e) {
                LOG.warn("Error populating target grants for bucket " + request.getBucket() + " for target " + bucket.getTargetBucket(), e);
                grants.setGrants(new ArrayList<Grant>());
            }

            loggingConfig.setTargetBucket(bucket.getTargetBucket());
            loggingConfig.setTargetPrefix(bucket.getTargetPrefix());
            loggingConfig.setTargetGrants(grants);
            reply.setLoggingEnabled(loggingConfig);
        } else {
            //Logging not enabled
            reply.setLoggingEnabled(null);
        }

        return reply;
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#SetBucketLoggingStatus(com.eucalyptus.objectstorage.msgs.SetBucketLoggingStatusType)
	 */
	@Override
	public SetBucketLoggingStatusResponseType setBucketLoggingStatus(final SetBucketLoggingStatusType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        //TODO: zhill -- add support for this. Not implemented for the tech preview
        throw new NotImplementedException("PUT ?logging");
	}

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#GetBucketVersioningStatus(com.eucalyptus.objectstorage.msgs.GetBucketVersioningStatusType)
	 */
	@Override
	public GetBucketVersioningStatusResponseType getBucketVersioningStatus(GetBucketVersioningStatusType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        //Metadata only, don't hit the backend
        GetBucketVersioningStatusResponseType reply = request.getReply();
        reply.setVersioningStatus(bucket.getVersioning().toString());
        reply.setBucket(request.getBucket());
        return reply;
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#SetBucketVersioningStatus(com.eucalyptus.objectstorage.msgs.SetBucketVersioningStatusType)
	 */
	@Override
	public SetBucketVersioningStatusResponseType setBucketVersioningStatus(final SetBucketVersioningStatusType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);
        ObjectStorageProperties.VersioningStatus versionStatus = ObjectStorageProperties.VersioningStatus.valueOf(request.getVersioningStatus());
        try {
            BucketMetadataManagers.getInstance().setVersioning(bucket, versionStatus);
        } catch(IllegalResourceStateException e) {
            throw new IllegalVersioningConfigurationException(request.getVersioningStatus());
        } catch(MetadataOperationFailureException e) {
            throw new InternalErrorException(e);
        } catch(NoSuchEntityException e) {
            throw new NoSuchBucketException(request.getBucket());
        }

        SetBucketVersioningStatusResponseType reply = request.getReply();
        return reply;
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#ListVersions(com.eucalyptus.objectstorage.msgs.ListVersionsType)
	 */
	@Override
	public ListVersionsResponseType listVersions(ListVersionsType request) throws S3Exception {
		Bucket bucket = getBucketAndCheckAuthorization(request);

        int maxKeys = ObjectStorageProperties.MAX_KEYS;
        if(!Strings.isNullOrEmpty(request.getMaxKeys())) {
            try {
                maxKeys = Integer.parseInt(request.getMaxKeys());
                if(maxKeys < 0 || maxKeys > ObjectStorageProperties.MAX_KEYS) {
                    throw new InvalidArgumentException(request.getMaxKeys());
                }
            } catch(NumberFormatException e) {
                throw new InvalidArgumentException(request.getMaxKeys());
            }
        }

        try {
            PaginatedResult<ObjectEntity> versionListing = ObjectMetadataManagers.getInstance().listVersionsPaginated(bucket,
                maxKeys,
                request.getPrefix(),
                request.getDelimiter(),
                request.getKeyMarker(),
                request.getVersionIdMarker(),
                false);

            ListVersionsResponseType reply = request.getReply();
            reply.setName(bucket.getBucketName());
            reply.setMaxKeys(maxKeys);
            reply.setKeyMarker(request.getKeyMarker());
            reply.setDelimiter(request.getDelimiter());
            reply.setPrefix(request.getPrefix());
            reply.setIsTruncated(versionListing.getIsTruncated());

            for(ObjectEntity ent : versionListing.getEntityList()) {
                reply.getKeyEntries().add(ent.toVersionEntry());
            }

            if(versionListing.getLastEntry() instanceof ObjectEntity) {
                reply.setNextKeyMarker(((ObjectEntity)versionListing.getLastEntry()).getObjectKey());
                reply.setNextVersionIdMarker(((ObjectEntity)versionListing.getLastEntry()).getVersionId());
            } else if(versionListing.getLastEntry() instanceof String) {
                //CommonPrefix entry
                reply.setNextKeyMarker(((String)versionListing.getLastEntry()));
            }

            for(String s : versionListing.getCommonPrefixes()) {
                reply.getCommonPrefixesList().add(new CommonPrefixesEntry(s));
            }

            return reply;

        } catch(S3Exception e) {
            throw e;
        } catch(Exception e) {
            LOG.warn("Error listing versions for bucket " + request.getBucket());
            throw new InternalErrorException(e);
        }
    }

	/* (non-Javadoc)
	 * @see com.eucalyptus.objectstorage.ObjectStorageService#DeleteVersion(com.eucalyptus.objectstorage.msgs.DeleteVersionType)
	 */
	@Override
	public DeleteVersionResponseType deleteVersion(final DeleteVersionType request) throws S3Exception {
        ObjectEntity objectEntity = getObjectEntityAndCheckPermissions(request, request.getVersionId());

        OsgObjectFactory.getFactory().logicallyDeleteVersion(ospClient, objectEntity, Contexts.lookup().getUser());

        DeleteVersionResponseType reply = request.getReply();
        reply.setBucket(request.getBucket());
        reply.setStatus(HttpResponseStatus.NO_CONTENT);
        reply.setKey(request.getKey());
        return reply;
    }

    @Override
    public GetBucketLifecycleResponseType getBucketLifecycle(GetBucketLifecycleType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        //Get the lifecycle from the back-end and copy results in.
        GetBucketLifecycleResponseType reply = (GetBucketLifecycleResponseType)request.getReply();
        try {
            LifecycleConfiguration lifecycle = new LifecycleConfiguration();
            List<LifecycleRule> responseRules
                    = BucketLifecycleManagers.getInstance().getLifecycleRules(bucket.getBucketUuid());
            lifecycle.setRules(responseRules);
            reply.setLifecycleConfiguration(lifecycle);
        } catch(Exception e) {
            throw new InternalErrorException( request.getBucket() );
        }
        return reply;

    }

    @Override
    public SetBucketLifecycleResponseType setBucketLifecycle(SetBucketLifecycleType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        SetBucketLifecycleResponseType response = request.getReply();
        String bucketName = request.getBucket();

        List<LifecycleRule> goodRules = new ArrayList<>();

        // per s3 docs, 1000 rules max, error matched with results from testing s3
        // validated that this rule gets checked prior to versioning checking
        if (request.getLifecycleConfiguration() != null
                && request.getLifecycleConfiguration().getRules() != null ) {

            if ( request.getLifecycleConfiguration().getRules().size() > 1000) {
                throw new MalformedXMLException(bucketName);
            }

            // make sure names are unique
            List<String> ruleIds = new ArrayList<>();
            String badId = null;
            for ( LifecycleRule rule : request.getLifecycleConfiguration().getRules()) {
                for (String ruleId : ruleIds) {
                    if (rule != null && (rule.getId() == null || rule.getId().equals(ruleId) )) {
                        badId = rule.getId() == null ? "null" : rule.getId();
                    }
                    else {
                        ruleIds.add(ruleId);
                    }
                    if (badId != null) {
                        break;
                    }
                }
                if (badId != null) {
                    InvalidArgumentException ex = new InvalidArgumentException(badId);
                    ex.setMessage("RuleId must be unique. Found same ID for more than one rule.");
                    throw ex;
                }
                else {
                    goodRules.add(rule);
                }
            }
        }

        if (!ObjectStorageProperties.VersioningStatus.Disabled.equals(bucket.getVersioning())) {
            throw new InvalidBucketStateException(bucketName);
        }

        try {
            BucketLifecycleManagers.getInstance().addLifecycleRules(goodRules, bucket.getBucketUuid());
        }
        catch ( Exception ex) {
            LOG.error("caught exception while managing object lifecycle for bucket - " +
                    bucketName + ", with error - " + ex.getMessage());
            throw new InternalErrorException(bucketName);
        }

        return response;

    }

    @Override
    public DeleteBucketLifecycleResponseType deleteBucketLifecycle(DeleteBucketLifecycleType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);
        DeleteBucketLifecycleResponseType response = request.getReply();
        try {
            BucketLifecycleManagers.getInstance().deleteLifecycleRules(bucket.getBucketUuid());
        }
        catch (Exception e) {
            InternalErrorException ex = new InternalErrorException(bucket.getBucketName() + "?lifecycle");
            ex.setMessage("An exception was caught while managing the object lifecycle for bucket - " + bucket.getBucketName());
            throw ex;
        }
        return response;

    }

    private Bucket getBucketAndCheckAuthorization(ObjectStorageRequestType request) throws S3Exception {
        logRequest(request);
        Bucket bucket = ensureBucketExists(request.getBucket());
        if (! OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, null, 0) ) {
            throw new AccessDeniedException(request.getBucket());
        }
        return bucket;
    }

    private ObjectEntity getObjectEntityAndCheckPermissions(ObjectStorageRequestType request, String versionId) throws S3Exception {
        logRequest(request);
        Bucket bucket = ensureBucketExists(request.getBucket());
        ObjectEntity object;
        String keyFullName = request.getBucket() + "/" + request.getKey() + (versionId == null ? "" : "?versionId=" + versionId);
        try {
            object = ObjectMetadataManagers.getInstance().lookupObject(bucket, request.getKey(), versionId);
        } catch (NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchKeyException(keyFullName);
        } catch (Exception e) {
            LOG.error("Error getting metadata for " + keyFullName );
            throw new InternalErrorException(keyFullName);
        }
        if (! OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, object, 0) ) {
            throw new AccessDeniedException(keyFullName);
        }
        return object;
    }

    private Bucket ensureBucketExists(String bucketName) throws S3Exception {
        Bucket bucket = null;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(bucketName);
        }
        catch (NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchBucketException(bucketName);
        }
        catch (Exception e) {
            LOG.error("Error getting metadata for bucket " + bucketName );
            throw new InternalErrorException(bucketName);
        }
        return bucket;
    }

    public InitiateMultipartUploadResponseType initiateMultipartUpload(InitiateMultipartUploadType request) throws S3Exception {
        logRequest(request);
        Bucket bucket = null;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
        } catch(NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchBucketException(request.getBucket());
        } catch(Exception e) {
            throw new InternalErrorException();
        }

        User requestUser = getRequestUser(request);
        ObjectEntity objectEntity;
        try {
            //Only create the entity for auth checks below, don't persist it
            objectEntity = ObjectEntity.newInitializedForCreate(bucket,
                    request.getKey(),
                    0,
                    requestUser);
        } catch (Exception e) {
            LOG.error("Error initializing entity for persisting object metadata for " + request.getBucket() + "/" + request.getKey());
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
        }

        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, objectEntity, 0)) {
        	final String originalBucket = request.getBucket();
            final String originalKey = request.getKey();
            try {
                AccessControlPolicy acp = getFullAcp(request.getAccessControlList(), requestUser, bucket.getOwnerCanonicalId());
                objectEntity.setAcl(acp);

                final String fullObjectKey = objectEntity.getObjectUuid();
                request.setKey(fullObjectKey); //Ensure the backend uses the new full object name
                request.setBucket(bucket.getBucketUuid());
                objectEntity = ObjectMetadataManagers.getInstance().initiateCreation(objectEntity);
                
                InitiateMultipartUploadResponseType response = ospClient.initiateMultipartUpload(request);
                objectEntity.setUploadId(response.getUploadId());
                response.setKey(originalKey);
                response.setBucket(originalBucket);
                ObjectMetadataManagers.getInstance().finalizeMultipartInit(objectEntity, new Date(), response.getUploadId());
                return response;
            } catch (Exception e) {
            	// Wrap the error from back-end with a 500 error
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
                throw new InternalErrorException(originalBucket + "/" + originalKey, e);
            }
        } else {
            throw new AccessDeniedException(request.getBucket() + "/" + request.getKey());
        }
    }

    public UploadPartResponseType uploadPart(final UploadPartType request) throws S3Exception {
        UploadPartResponseType reply = (UploadPartResponseType) request.getReply();
        logRequest(request);
        Bucket bucket = null;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
        } catch(NoSuchEntityException e) {
            throw new NoSuchBucketException(request.getBucket());
        } catch(Exception e) {
            throw new InternalErrorException();
        }

        if(Strings.isNullOrEmpty(request.getContentLength())) {
            //Not known. Content-Length is required by S3-spec.
            throw new MissingContentLengthException(request.getBucket() + "/" + request.getKey());
        }

        long objectSize = 0;
        try {
            objectSize = Long.parseLong(request.getContentLength());
        } catch(Exception e) {
            LOG.error("Could not parse content length into a long: " + request.getContentLength(), e);
            throw new MissingContentLengthException(request.getBucket() + "/" + request.getKey());
        }

        User requestUser = Contexts.lookup().getUser();
        PartEntity partEntity;
        try {
            partEntity = PartEntity.newInitializedForCreate(bucket,
                    request.getKey(),
                    request.getUploadId(),
                    Integer.parseInt(request.getPartNumber()),
                    objectSize,
                    requestUser);
        } catch (Exception e) {
            LOG.error("Error initializing entity for persisting part metadata for "
                    + request.getBucket() + "/" + request.getKey()
                    + " uploadId: " + request.getUploadId()
                    + " partNumber: " + request.getPartNumber());
            throw new InternalErrorException(request.getBucket() + "/" + request.getKey());
        }
        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, partEntity, objectSize)) {
            //Auth worked, check if we need to send a 100-continue
            try {
                if (request.getExpectHeader()) {
                    OSGChannelWriter.writeResponse(Contexts.lookup(request.getCorrelationId()), OSGMessageResponse.Continue);
                }
            } catch (Exception e) {
                throw new InternalErrorException(e);
            }

            ObjectEntity objectEntity;
            try {
                objectEntity = ObjectMetadataManagers.getInstance().lookupUpload(bucket, request.getUploadId());
            } catch (Exception e) {
                throw new NoSuchUploadException("Cannot get upload for: " + bucket.getBucketName() + "/" + request.getKey());
            }
            try {
                PartEntity updatedEntity = OsgObjectFactory.getFactory().createObjectPart(ospClient, objectEntity, partEntity, request.getData(), requestUser);
                UploadPartResponseType response = request.getReply();
                response.setLastModified(updatedEntity.getObjectModifiedTimestamp());
                response.setEtag(updatedEntity.geteTag());
                response.setStatusMessage("OK");
                response.setSize(updatedEntity.getSize());
                return response;
            } catch (Exception e) {
            	// Wrap the error from back-end with a 500 error
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
                throw new InternalErrorException(partEntity.getResourceFullName(), e);
            }
        } else {
            throw new AccessDeniedException(request.getBucket());
        }
    }

    public CompleteMultipartUploadResponseType completeMultipartUpload(final CompleteMultipartUploadType request) throws S3Exception {
        logRequest(request);
        Bucket bucket;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
        } catch(NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchBucketException(request.getBucket());
        } catch(Exception e) {
            throw new InternalErrorException();
        }

        ObjectEntity objectEntity;
        User requestUser = Contexts.lookup().getUser();
        try {
            objectEntity = ObjectMetadataManagers.getInstance().lookupUpload(bucket, request.getUploadId());
        } catch(NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchUploadException(request.getUploadId());
        } catch (Exception e) {
            throw new InternalErrorException("Cannot get size for uploaded parts for: " + bucket.getBucketName() + "/" + request.getKey());
        }

        long newBucketSize = bucket.getBucketSize() == null ? 0 : bucket.getBucketSize(); //No change, completion cannot increase the size of the bucket, only decrease it.
        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, objectEntity, newBucketSize)) {
            try {
                objectEntity = ObjectMetadataManagers.getInstance().lookupUpload(bucket, request.getUploadId());
            } catch (Exception e) {
                throw new NoSuchUploadException("Cannot get upload for: " + bucket.getBucketName() + "/" + request.getKey());
            }
            try {
                //TODO: need to add the necesary logic to hold the connection open by sending ' ' on the channel periodically
                //The backend operation could take a while.
                ObjectEntity completedEntity = OsgObjectFactory.getFactory().completeMultipartUpload(ospClient, objectEntity, request.getParts(), requestUser);
                CompleteMultipartUploadResponseType response = request.getReply();
                response.setSize(completedEntity.getSize());
                response.setEtag(completedEntity.geteTag());
                response.setLastModified(completedEntity.getObjectModifiedTimestamp());
                response.setLocation(Topology.lookup(ObjectStorage.class).getUri() + "/" + completedEntity.getBucket().getBucketName() + "/" + completedEntity.getObjectKey());
                response.setBucket(request.getBucket());
                response.setKey(request.getKey());
                return response;
            } catch (Exception e) {
            	// Wrap the error from back-end with a 500 error
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
                throw new InternalErrorException(objectEntity.getResourceFullName(), e);
            }
        } else {
            throw new AccessDeniedException(request.getBucket() + "/" + request.getKey());
        }

    }

    public AbortMultipartUploadResponseType abortMultipartUpload(AbortMultipartUploadType request) throws S3Exception {
        logRequest(request);
        ObjectEntity objectEntity;
        Bucket bucket;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(request.getBucket());
        } catch(NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchBucketException(request.getBucket());
        } catch(Exception e) {
            throw new InternalErrorException(e.getMessage());
        }
        try {
            objectEntity = ObjectMetadataManagers.getInstance().lookupUpload(bucket, request.getUploadId());
            //convert to uuid, which corresponding to the key on the backend
            request.setKey(objectEntity.getObjectUuid());
            request.setBucket(bucket.getBucketUuid());
        } catch(NoSuchEntityException | NoSuchElementException e) {
            throw new NoSuchUploadException(request.getUploadId());
        } catch(Exception e) {
            throw new InternalErrorException(e.getMessage());
        }
        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, objectEntity, 0)) {
            ObjectMetadataManagers.getInstance().transitionObjectToState(objectEntity, ObjectState.deleting);
            try {
            	AbortMultipartUploadResponseType response = ospClient.abortMultipartUpload(request);
            	User requestUser = Contexts.lookup().getUser();

            	//all okay, delete all parts
                OsgObjectFactory.getFactory().flushMultipartUpload(ospClient, objectEntity, requestUser);
                return response;
            } catch (Exception e) {
            	// Wrap the error from back-end with a 500 error
            	LOG.warn("CorrelationId: " + Contexts.lookup().getCorrelationId() + " Responding to client with 500 InternalError because of:", e);
                throw new InternalErrorException("Could not remove parts for: " + request.getUploadId());
            }
        } else {
            throw new AccessDeniedException(request.getBucket() + "/" + request.getKey());
        }
    }

    /*
     * Return parts for a given multipart request
     *
     */
    public ListPartsResponseType listParts(ListPartsType request) throws S3Exception {
        logRequest(request);
        ListPartsResponseType reply = request.getReply();
        String bucketName = request.getBucket();
        String objectKey = request.getKey();
        ObjectEntity objectEntity;
        Bucket bucket;
        try {
            bucket = BucketMetadataManagers.getInstance().lookupExtantBucket(bucketName);
        } catch(NoSuchElementException e) {
            throw new NoSuchBucketException(request.getBucket());
        } catch(Exception e) {
            throw new InternalErrorException(e.getMessage());
        }
        try {
            objectEntity = ObjectMetadataManagers.getInstance().lookupUpload(bucket, request.getUploadId());
        } catch(NoSuchElementException e) {
            throw new NoSuchKeyException(request.getBucket() + "/" + request.getKey());
        } catch(Exception e) {
            throw new InternalErrorException(e.getMessage());
        }

        if(OsgAuthorizationHandler.getInstance().operationAllowed(request, bucket, objectEntity, 0)) {
            int maxParts = 1000;
            try {
                if(request.getMaxParts() != null) {
                    maxParts = request.getMaxParts();
                }
            } catch(NumberFormatException e) {
                LOG.error("Failed to parse max parts from request properly: " + request.getMaxParts(), e);
                throw new InvalidArgumentException("maxParts");
            }

            try {
                Initiator initiator = new Initiator(
                        objectEntity.getOwnerIamUserId(),
                        objectEntity.getOwnerIamUserDisplayName());
                reply.setInitiator(initiator);
                CanonicalUser owner = new CanonicalUser(
                        objectEntity.getOwnerCanonicalId(),
                        objectEntity.getOwnerDisplayName());
                reply.setOwner(owner);
                reply.setStorageClass(objectEntity.getStorageClass());
                reply.setPartNumberMarker(request.getPartNumberMarker());
                reply.setMaxParts(request.getMaxParts());
                reply.setBucket(bucketName);
                reply.setKey(objectKey);
                reply.setUploadId(request.getUploadId());
            } catch (Exception e) {
                throw new NoSuchUploadException(request.getUploadId());
            }
            try {
                PaginatedResult<PartEntity> result = MpuPartMetadataManagers.getInstance().listPartsForUpload(bucket, objectKey, request.getUploadId(), request.getPartNumberMarker(), maxParts);
                reply.setIsTruncated(result.getIsTruncated());
                if(	result.getLastEntry() instanceof PartEntity) {
                    reply.setNextPartNumberMarker(((PartEntity)result.getLastEntry()).getPartNumber());
                }
                for (PartEntity entity : result.getEntityList()) {
                    List<Part> replyParts = reply.getParts();
                    replyParts.add(entity.toPartListEntry());
                }
            } catch (Exception e) {
                throw new InternalErrorException(e.getMessage());
            }
            return reply;
        } else {
            throw new AccessDeniedException(request.getBucket() + "/" + request.getKey());
        }

    }

    /*
     * Return all active multipart uploads for a bucket
     *
     */
    public ListMultipartUploadsResponseType listMultipartUploads(ListMultipartUploadsType request) throws S3Exception {
        Bucket bucket = getBucketAndCheckAuthorization(request);

        int maxUploads = ObjectStorageProperties.MAX_KEYS;
        try {
            if(request.getMaxUploads() != null) {
                maxUploads = request.getMaxUploads();
            }
        } catch(NumberFormatException e) {
            LOG.error("Failed to parse max uploads from request properly: " + request.getMaxUploads(), e);
            throw new InvalidArgumentException("MaxKeys");
        }


        ListMultipartUploadsResponseType reply = request.getReply();
        reply.setMaxUploads(maxUploads);
        reply.setBucket(request.getBucket());
        reply.setDelimiter(request.getDelimiter());
        reply.setKeyMarker(request.getKeyMarker());
        reply.setPrefix(request.getPrefix());
        reply.setIsTruncated(false);

        PaginatedResult<ObjectEntity> result;
        try {
            result = ObjectMetadataManagers.getInstance().listUploads(bucket, maxUploads, request.getPrefix(), request.getDelimiter(), request.getKeyMarker(), request.getUploadIdMarker());
        } catch(Exception e) {
            LOG.error("Error getting object listing for bucket: " + request.getBucket(), e);
            throw new InternalErrorException(request.getBucket());
        }

        if(result != null) {
            reply.setUploads(new ArrayList<Upload>());

            for(ObjectEntity obj : result.getEntityList()){
                reply.getUploads().add(new Upload(obj.getObjectKey(), obj.getUploadId(),
                        new Initiator(obj.getOwnerIamUserId(), obj.getOwnerIamUserDisplayName()),
                        new CanonicalUser(obj.getOwnerCanonicalId(), obj.getOwnerDisplayName()),
                        obj.getStorageClass(), obj.getCreationTimestamp()));
            }

            if(result.getCommonPrefixes() != null && result.getCommonPrefixes().size() > 0) {
                reply.setCommonPrefixes(new ArrayList<CommonPrefixesEntry>());

                for(String s : result.getCommonPrefixes()) {
                    reply.getCommonPrefixes().add(new CommonPrefixesEntry(s));
                }
            }
            reply.setIsTruncated(result.isTruncated);
            if(result.isTruncated) {
                if(	result.getLastEntry() instanceof ObjectEntity) {
                    reply.setNextKeyMarker(((ObjectEntity)result.getLastEntry()).getObjectKey());
                } else {
                    //If max-keys = 0, then last entry may be empty
                    reply.setNextKeyMarker((result.getLastEntry() != null ? result.getLastEntry().toString() : ""));
                }
            }
        }

        return reply;
    }

    /**
     * Fire creation and possibly a related delete event.
     *
     * If an object (version) is being overwritten then there will not be a corresponding
     * delete event so we fire one prior to the create event.
     */
    private void fireObjectCreationEvent(final String bucketName, final String objectKey, final String version,
                                         final String userId, final Long size, final Long oldSize) {
        try {
            if (oldSize != null && oldSize > 0) {
                fireObjectUsageEvent(S3ObjectEvent.S3ObjectAction.OBJECTDELETE, bucketName, objectKey, version, userId, oldSize);
            }

			/* Send an event to reporting to report this S3 usage. */
            if (size != null && size > 0) {
                fireObjectUsageEvent(S3ObjectEvent.S3ObjectAction.OBJECTCREATE, bucketName, objectKey, version, userId, size);
            }
        } catch (final Exception e) {
            LOG.error(e, e);
        }
    }

    private void fireObjectUsageEvent(S3ObjectEvent.S3ObjectAction actionInfo, String bucketName,
                                             String objectKey, String version, String ownerUserId,
                                             Long sizeInBytes) {
        try {
            ListenerRegistry.getInstance().fireEvent(S3ObjectEvent.with(actionInfo, bucketName, objectKey, version, ownerUserId, sizeInBytes));
        } catch (final Exception e) {
            LOG.error(e, e);
        }
    }
}
