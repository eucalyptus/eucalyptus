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

import com.eucalyptus.objectstorage.util.ObjectStorageProperties.BucketParameter;
import com.eucalyptus.objectstorage.util.ObjectStorageProperties.ObjectParameter;
import com.google.common.collect.ImmutableMap;

public class ObjectStorageGETBinding extends ObjectStorageRESTBinding {

  private static final ImmutableMap<String, String> SUPPORTED_OPS = ImmutableMap.<String,String>builder( )
    // Service operations
    .put(SERVICE + HttpMethod.GET, "ListAllMyBuckets")

    // Bucket operations
    .put(BUCKET + HttpMethod.GET + BucketParameter.acl.toString(), "GetBucketAccessControlPolicy")
    .put(BUCKET + HttpMethod.GET, "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.prefix.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.maxkeys.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.marker.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.delimiter.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.startafter.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.continuationtoken.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.fetchowner.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.listtype.toString(), "ListBucket")
    .put(BUCKET + HttpMethod.GET + BucketParameter.location.toString(), "GetBucketLocation")
    .put(BUCKET + HttpMethod.GET + BucketParameter.logging.toString(), "GetBucketLoggingStatus")
    .put(BUCKET + HttpMethod.GET + BucketParameter.versions.toString(), "ListVersions")
    .put(BUCKET + HttpMethod.GET + BucketParameter.versioning.toString(), "GetBucketVersioningStatus")
    .put(BUCKET + HttpMethod.GET + BucketParameter.lifecycle.toString(), "GetBucketLifecycle")
    .put(BUCKET + HttpMethod.GET + BucketParameter.tagging.toString(), "GetBucketTagging")
    .put(BUCKET + HttpMethod.GET + BucketParameter.cors.toString(), "GetBucketCors")
    .put(BUCKET + HttpMethod.GET + BucketParameter.policy.toString(), "GetBucketPolicy")
    .put(BUCKET + HttpMethod.GET + BucketParameter.accelerate.toString(), "GetBucketAccelerateConfiguration")
    .put(BUCKET + HttpMethod.GET + BucketParameter.requestPayment.toString().toLowerCase(), "GetBucketRequestPayment")

    // Multipart uploads
    .put(BUCKET + HttpMethod.GET + BucketParameter.uploads.toString(), "ListMultipartUploads")

    // Object operations
    .put(OBJECT + HttpMethod.GET + ObjectParameter.acl.toString(), "GetObjectAccessControlPolicy")
    .put(OBJECT + HttpMethod.GET, "GetObject")
    .put(OBJECT + HttpMethod.GET + ObjectParameter.torrent.toString(), "GetObject")
    .put(OBJECT + HttpMethod.GET + "extended", "GetObjectExtended")

    // Multipart Uploads
    .put(OBJECT + HttpMethod.GET + ObjectParameter.uploadId.toString().toLowerCase(), "ListParts")
    .build( );

  private static final ImmutableMap<String, String> UNSUPPORTED_OPS = ImmutableMap.<String,String>builder( )
    // Bucket operations
    // Notification
    .put(BUCKET + HttpMethod.GET + BucketParameter.notification.toString(), "GET Bucket notification")
    // Website
    .put(BUCKET + HttpMethod.GET + BucketParameter.website.toString(), "GET Bucket website")
    // Metrics
    .put(BUCKET + HttpMethod.GET + BucketParameter.metrics.toString(), "GET Bucket metrics")
    // Analytics
    .put(BUCKET + HttpMethod.GET + BucketParameter.analytics.toString(), "GET Bucket analytics")
    // Inventory
    .put(BUCKET + HttpMethod.GET + BucketParameter.inventory.toString(), "GET Bucket inventory")
    // Replication
    .put(BUCKET + HttpMethod.GET + BucketParameter.replication.toString(), "GET Bucket replication")
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
