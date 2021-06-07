/*************************************************************************
 * Copyright 2008 Regents of the University of California
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

package com.eucalyptus.objectstorage.pipeline.binding;

import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpMethod;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.BucketParameter;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.ObjectParameter;
import com.google.common.collect.ImmutableMap;

public class ObjectStoragePUTBinding extends ObjectStorageRESTBinding {

  private static final ImmutableMap<String, String> SUPPORTED_OPS = ImmutableMap.<String,String>builder( )
    // Bucket operations
    .put(BUCKET + HttpMethod.PUT, "CreateBucket")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.acl.toString(), "SetBucketAccessControlPolicy")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.logging.toString(), "SetBucketLoggingStatus")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.versioning.toString(), "SetBucketVersioningStatus")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.lifecycle.toString(), "SetBucketLifecycle")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.tagging.toString(), "SetBucketTagging")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.cors.toString(), "SetBucketCors")
    .put(BUCKET + HttpMethod.PUT + BucketParameter.policy.toString(), "SetBucketPolicy")

    .put(BUCKET + HttpMethod.POST + BucketParameter.delete, "DeleteMultipleObjects")

    // Object operations
    .put(OBJECT + HttpMethod.PUT, "PutObject")
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.acl.toString(), "SetObjectAccessControlPolicy")
    .put(OBJECT + HttpMethod.PUT + ObjectStorageProperties.COPY_SOURCE, "CopyObject")

    // Multipart Uploads
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.partNumber.toString().toLowerCase()
        + ObjectParameter.uploadId.toString().toLowerCase() , "UploadPart")
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.uploadId.toString().toLowerCase()
        + ObjectParameter.partNumber.toString().toLowerCase(), "UploadPart")

    .put(OBJECT + HttpMethod.PUT + ObjectParameter.uploadId.toString().toLowerCase()
        + ObjectParameter.partNumber.toString().toLowerCase() + ObjectStorageProperties.COPY_SOURCE,
        "UploadPartCopy")
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.uploadId.toString().toLowerCase()
        + ObjectStorageProperties.COPY_SOURCE + ObjectParameter.partNumber.toString().toLowerCase(),
        "UploadPartCopy")
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.partNumber.toString().toLowerCase()
        + ObjectParameter.uploadId.toString().toLowerCase() + ObjectStorageProperties.COPY_SOURCE,
        "UploadPartCopy")
    .put(OBJECT + HttpMethod.PUT + ObjectParameter.partNumber.toString().toLowerCase()
        + ObjectStorageProperties.COPY_SOURCE + ObjectParameter.uploadId.toString().toLowerCase(),
        "UploadPartCopy")
    .put(OBJECT + HttpMethod.PUT + ObjectStorageProperties.COPY_SOURCE
        + ObjectParameter.uploadId.toString().toLowerCase()
        + ObjectParameter.partNumber.toString().toLowerCase(),
        "UploadPartCopy")
    .put(OBJECT + HttpMethod.PUT + ObjectStorageProperties.COPY_SOURCE
        + ObjectParameter.partNumber.toString().toLowerCase()
        + ObjectParameter.uploadId.toString().toLowerCase(),
        "UploadPartCopy")

    .put(OBJECT + HttpMethod.POST + ObjectParameter.uploads.toString(), "InitiateMultipartUpload")
    .put(OBJECT + HttpMethod.POST + ObjectParameter.uploadId.toString().toLowerCase(), "CompleteMultipartUpload")
    .build( );

  private static final ImmutableMap<String, String> UNSUPPORTED_OPS = ImmutableMap.<String,String>builder( )
    // Bucket operations
    // Notification
    .put(BUCKET + HttpMethod.PUT + BucketParameter.notification.toString(), "PUT Bucket notification")
    // Website
    .put(BUCKET + HttpMethod.PUT + BucketParameter.website.toString(), "PUT Bucket website")
    // Accelerate
    .put(BUCKET + HttpMethod.PUT + BucketParameter.accelerate.toString(), "PUT Bucket accelerate")
    // Metrics
    .put(BUCKET + HttpMethod.PUT + BucketParameter.metrics.toString(), "PUT Bucket metrics")
    // Analytics
    .put(BUCKET + HttpMethod.PUT + BucketParameter.analytics.toString(), "PUT Bucket analytics")
    // Inventory
    .put(BUCKET + HttpMethod.PUT + BucketParameter.inventory.toString(), "PUT Bucket inventory")
    // Replication
    .put(BUCKET + HttpMethod.PUT + BucketParameter.replication.toString(), "PUT Bucket replication")
    .build( );

  @Override
  protected Map<String, String> populateOperationMap() {
    return SUPPORTED_OPS;
  }

  @Override
  protected Map<String, String> populateUnsupportedOperationMap() {
    return UNSUPPORTED_OPS;
  }
}
