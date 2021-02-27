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

package com.eucalyptus.objectstorage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.eucalyptus.auth.principal.User;
import com.eucalyptus.auth.principal.UserPrincipal;
import com.eucalyptus.objectstorage.entities.ObjectEntity;
import com.eucalyptus.objectstorage.entities.PartEntity;
import com.eucalyptus.objectstorage.exceptions.s3.S3Exception;
import com.eucalyptus.objectstorage.msgs.CopyObjectType;
import com.eucalyptus.objectstorage.msgs.UploadPartCopyType;
import com.eucalyptus.objectstorage.providers.ObjectStorageProviderClient;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;
import com.eucalyptus.storage.msgs.s3.Part;

public interface ObjectFactory {
  /**
   * Create the named object in metadata and on the backend.
   * 
   * @return the ObjectEntity object representing the successfully created object
   */
  public ObjectEntity createObject(ObjectStorageProviderClient provider, ObjectEntity entity, InputStream content, List<MetaDataEntry> metadata,
      User requestUser) throws S3Exception;

  /**
   * Create the named object in metadata and on the backend, based on the original CopyObject request.
   * 
   * @return the ObjectEntity object representing the successfully copied object
   */
  public ObjectEntity copyObject(ObjectStorageProviderClient provider, ObjectEntity entity, CopyObjectType request, User requestUser,
      String metadataDirective) throws S3Exception;

  /**
   * Logically delete the object. This is the preferred method invocation as a result of a user request. Marks the object delete, may also insert a
   * deleteMarker or other metadata operation to indicate the object is logically deleted upon return. It may return the deleteMarker if it was
   * created. If no delete marker was inserted, nothing is returned
   *
   * @param entity
   * @param requestUser
   * @return objectEntity
   * @throws S3Exception
   */
  public ObjectEntity logicallyDeleteObject(ObjectStorageProviderClient provider, ObjectEntity entity, UserPrincipal requestUser) throws S3Exception;

  /**
   * Delete a specific version. Differs from logicallyDeleteObject in that it will never generate a delete marker, but will operate on that specific
   * version directly and either remove it if a deletemarker, or transition it to 'deleting'. It will return the object entity version that was
   * removed.
   * 
   * @param entity
   * @param requestUser
   * @return objectEntity
   * @throws S3Exception
   */
  public ObjectEntity logicallyDeleteVersion(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception;

  /**
   * Delete the named bucket in metadata and on the backend. This is intended for usage by async processes such as object GC, should probably not be
   * on a sync call path from user.
   * 
   * @param entity ObjectEntity record for object to delete
   */
  public void actuallyDeleteObject(ObjectStorageProviderClient provider, ObjectEntity entity, User requestUser) throws S3Exception;

  /**
   * Create the named object part in metadata and on the backend.
   * 
   * @return the ObjectEntity object representing the successfully created object
   */
  public PartEntity createObjectPart(ObjectStorageProviderClient provider, ObjectEntity mpuEntity, PartEntity entity, InputStream content,
      User requestUser) throws S3Exception;

  /**
   * Create the named object part in metadata and on the backend.
   *
   * @return the PartEntity object representing the successfully created part
   */
  public PartEntity copyObjectPart(ObjectStorageProviderClient provider, ObjectEntity mpuEntity, PartEntity entity, UploadPartCopyType request,
      User requestUser) throws S3Exception;

  /**
   * Create a multipart Upload (get an Id from the backend and initialize the metadata. Returns a persisted uploadId record as an ObjectEntity with
   * the uploadId in state 'mpu-pending'
   * 
   * @param provider
   * @param upload
   * @param requestUser
   * @return
   * @throws S3Exception
   */
  public ObjectEntity createMultipartUpload(final ObjectStorageProviderClient provider, ObjectEntity upload, User requestUser) throws S3Exception;

  /**
   * Commits a Multipart Upload into an extant object entity.
   * 
   * @param provider
   * @param mpuEntity the ObjectEntity that is the upload parent record, as supplied by the ObjectMetadataManager.lookupUpload()
   * @param requestUser
   * @return
   * @throws S3Exception
   */
  public ObjectEntity completeMultipartUpload(ObjectStorageProviderClient provider, ObjectEntity mpuEntity, ArrayList<Part> partList, User requestUser)
      throws S3Exception;

  /**
   * Flushes the mulitpart upload and all artifacts that are not committed.
   * 
   * @param uploadEntity ObjectEntity record for object to delete, the parent MPU record
   */
  public void flushMultipartUpload(ObjectStorageProviderClient provider, ObjectEntity uploadEntity, User requestUser) throws S3Exception;

}
