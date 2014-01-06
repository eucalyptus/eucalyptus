package com.eucalyptus.objectstorage;

import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.principal.Principals;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.DeleteObjectResponseType;
import com.eucalyptus.objectstorage.msgs.DeleteObjectType;
import com.eucalyptus.util.EucalyptusCloudException;

/**
 * Scans metadata for "deleted" objects and removes them from the backend.
 * Many of these may be running concurrently.
 *
 */
public class ObjectReaperTask implements Runnable {
	private static final Logger LOG = Logger.getLogger(ObjectReaperTask.class);
		
	public ObjectReaperTask() {}
	
	public void reapObject(final ObjectEntity obj) throws Exception {
		DeleteObjectType deleteRequest = null;
		DeleteObjectResponseType deleteResponse = null;
		ObjectStorageProviderClient client = ObjectStorageProviders.getInstance();		
		LOG.trace("Reaping " + obj.getBucketName() + "/" + obj.getObjectUuid() + ".");
		deleteRequest = new DeleteObjectType();
		User requestUser = null;
		try {
			requestUser = Accounts.lookupUserById(obj.getOwnerIamUserId());
		} catch(AuthException e) {
			//user doesn't exist, use account admin
			LOG.trace("User with id " + obj.getOwnerIamUserId() + " not found during object reaping. Trying account admin for canonicalId " + obj.getOwnerCanonicalId());
			try {
				requestUser = Accounts.lookupAccountByCanonicalId(obj.getOwnerCanonicalId()).lookupAdmin();
			} catch(AuthException ex) {
				LOG.trace("Account admin for canonicalId " + obj.getOwnerCanonicalId() + " not found. Cannot remove object with uuid " + obj.getBucketName() + "/" + obj.getObjectUuid());
				throw ex;
			}
		}
		
		if(requestUser.getKeys() != null && requestUser.getKeys().size() > 0) {
			deleteRequest.setAccessKeyID(requestUser.getKeys().get(0).getAccessKey());
		} else {
			LOG.trace("No access keys found for user " + requestUser.getUserId() + " using admin accound for user");
			User admin = requestUser.getAccount().lookupAdmin();
			if(admin.getKeys() != null &&  admin.getKeys().size() > 0) {
				deleteRequest.setAccessKeyID(admin.getKeys().get(0).getAccessKey());
			} else {
				LOG.error("Cannot find a valid AccessKeyId for backend request for user " + requestUser.getUserId() + " or account " + requestUser.getAccount().getAccountNumber() + " admin");
				throw new AuthException("Could not setup auth properly for delete request");
			}
		}
		
		deleteRequest.setUser(requestUser);
		deleteRequest.setBucket(obj.getBucketName());
		deleteRequest.setKey(obj.getObjectUuid());
		
		try {
			deleteResponse = client.deleteObject(deleteRequest);
			if(HttpResponseStatus.NO_CONTENT.equals(deleteResponse.getStatus()) || HttpResponseStatus.OK.equals(deleteResponse.getStatus())) {
				//Object does not exist on backend, remove record
				Transactions.delete(obj);
			} else {
				LOG.trace("Backend did not confirm deletion of " + deleteRequest.getBucket() + "/" + deleteRequest.getKey() + " via request: " + deleteRequest.toString());
				throw new Exception("Object could not be confirmed as deleted.");
			}
		} catch(EucalyptusCloudException ex) {
			//Failed. Keep record so we can retry later
			LOG.trace("Error in response from backend on deletion request for object on backend: " + deleteRequest.getBucket() + "/" + deleteRequest.getKey());						
		}
	}
	
	//Does a single scan of the DB and reclaims objects it finds in the 'deleting' state
	@Override
	public void run() {
		long startTime = System.currentTimeMillis();
		try {
			LOG.debug("Initiating object-storage object reaper task");
			List<ObjectEntity> entitiesToClean = ObjectManagers.getInstance().getFailedOrDeleted();
			DeleteObjectType deleteRequest = null;
			DeleteObjectResponseType deleteResponse = null;
			ObjectStorageProviderClient client = ObjectStorageProviders.getInstance();
			if(client == null) {
				LOG.error("Provider client for ObjectReaperTask is null. Cannot execute.");
				return;
			}
			LOG.trace("Reaping " + entitiesToClean.size() + " objects from backend");
			for(ObjectEntity obj : entitiesToClean) {
				try {
					reapObject(obj);
				} catch(final Throwable f) {
					LOG.error("Error during object reaper cleanup for object: " + 
							obj.getBucketName() + "/" + obj.getObjectKey() + "versionId=" + obj.getVersionId() + 
							" uuid= " + obj.getObjectUuid(), f);
				}
			}
		} catch(final Throwable f) {
			LOG.error("Error during object reaper execution. Will retry later", f);			
		} finally {
			try {
				long endTime = System.currentTimeMillis();
				LOG.debug("Object reaper execution task took " + Long.toString(endTime - startTime) + "ms to complete");
			} catch( final Throwable f) {
				//Do nothing, but don't allow exceptions out
			}
		}
	}
}
