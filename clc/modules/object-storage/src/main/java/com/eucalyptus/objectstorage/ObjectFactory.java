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

import java.io.InputStream;
import java.nio.channels.Channel;
import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;

public interface ObjectFactory {
	/**
	 * Create the named object in metadata and on the backend.
	 * @return the ObjectEntity object representing the successfully created object
	 */
	public ObjectEntity createObject(ObjectStorageProviderClient provider, ObjectEntity entity, InputStream content, List<MetaDataEntry> metadata, User requestUser) throws S3Exception;

    /**
     * Create the named object in metadata and on the backend, based on the original CopyObject request.
     * @return the ObjectEntity object representing the successfully copied object
     */
    public ObjectEntity copyObject(ObjectStorageProviderClient provider, ObjectEntity entity, CopyObjectType request, User requestUser, String metadataDirective) throws S3Exception;

    /**
     * Logically delete the object. This is the preferred method invocation as a result of a user request.
     * Marks the object delete, may also insert a deleteMarker or other metadata operation to indicate the object
     * is logically deleted upon return
     *
     * @param entity
     * @param requestUser
     * @throws S3Exception
     */
    public void logicallyDeleteObject(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception;

    /**
     * Delete a specific version. Differs from logicallyDeleteObject in that it will never generate a delete marker,
     * but will operate on that specific version directly and either remove it if a deletemarker, or transition it
     * to 'deleting'
     * @param entity
     * @param requestUser
     * @throws S3Exception
     */
    public void logicallyDeleteVersion(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception;

        /**
         * Delete the named bucket in metadata and on the backend.
         * This is intended for usage by async processes such as object GC, should probably not be on a sync call path from user.
         * @param entity ObjectEntity record for object to delete
         */
	public void actuallyDeleteObject(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception;

    /**
     * Create the named object part in metadata and on the backend.
     * @return the ObjectEntity object representing the successfully created object
     */
    public PartEntity createObjectPart(ObjectStorageProviderClient provider, ObjectEntity mpuEntity, PartEntity entity, InputStream content, User requestUser) throws S3Exception;

    /**
     * Create a multipart Upload (get an Id from the backend and initialize the metadata.
     * Returns a persisted uploadId record as an ObjectEntity with the uploadId in state 'mpu-pending'
     * @param provider
     * @param upload
     * @param requestUser
     * @return
     * @throws S3Exception
     */
    public ObjectEntity createMultipartUpload(final ObjectStorageProviderClient provider, ObjectEntity upload, User requestUser) throws S3Exception;

    /**
     * Commits a Multipart Upload into an extant object entity.
     * @param provider
     * @param mpuEntity the ObjectEntity that is the upload parent record, as supplied by the ObjectMetadataManager.lookupUpload()
     * @param requestUser
     * @return
     * @throws S3Exception
     */
    public ObjectEntity completeMultipartUpload(ObjectStorageProviderClient provider, ObjectEntity mpuEntity, ArrayList<Part> partList, User requestUser) throws S3Exception;

    /**
     * Flushes the mulitpart upload and all artifacts that are not committed.
     * @param uploadEntity ObjectEntity record for object to delete, the parent MPU record
     */
    public void flushMultipartUpload(ObjectStorageProviderClient provider, ObjectEntity uploadEntity, User requestUser) throws S3Exception;

}
