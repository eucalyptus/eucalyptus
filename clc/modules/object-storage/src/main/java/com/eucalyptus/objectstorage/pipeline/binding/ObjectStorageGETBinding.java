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

import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpMethod;

import com.eucalyptus.objectstorage.util.ObjectStorageProperties;
import com.google.common.collect.Maps;

public class ObjectStorageGETBinding extends ObjectStorageRESTBinding {
  private static Logger LOG = Logger.getLogger(ObjectStorageGETBinding.class);

  private static final Map<String, String> SUPPORTED_OPS = Maps.newHashMap();
  static {
    // Service operations
    SUPPORTED_OPS.put(SERVICE + HttpMethod.GET.toString(), "ListAllMyBuckets");

    // Bucket operations
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.acl.toString(), "GetBucketAccessControlPolicy");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.prefix.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.maxkeys.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.marker.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.delimiter.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.startafter.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.continuationtoken.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.fetchowner.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.listtype.toString(), "ListBucket");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.location.toString(), "GetBucketLocation");

    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.logging.toString(), "GetBucketLoggingStatus");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.versions.toString(), "ListVersions");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.versioning.toString(),
        "GetBucketVersioningStatus");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.lifecycle.toString(), "GetBucketLifecycle");
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.tagging.toString(), "GetBucketTagging");
    // Cross-Origin Resource Sharing (CORS)
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.cors.toString(), "GetBucketCors");

    // Multipart uploads
    SUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.uploads.toString(), "ListMultipartUploads");

    // Object operations
    SUPPORTED_OPS.put(OBJECT + HttpMethod.GET.toString() + ObjectStorageProperties.ObjectParameter.acl.toString(), "GetObjectAccessControlPolicy");
    SUPPORTED_OPS.put(OBJECT + HttpMethod.GET.toString(), "GetObject");
    SUPPORTED_OPS.put(OBJECT + HttpMethod.GET.toString() + ObjectStorageProperties.ObjectParameter.torrent.toString(), "GetObject");
    SUPPORTED_OPS.put(OBJECT + HttpMethod.GET.toString() + "extended", "GetObjectExtended");

    // Multipart Uploads
    SUPPORTED_OPS.put(OBJECT + HttpMethod.GET.toString() + ObjectStorageProperties.ObjectParameter.uploadId.toString().toLowerCase(), "ListParts");
  }

  private static final Map<String, String> UNSUPPORTED_OPS = Maps.newHashMap();
  static {
    // Bucket operations
    // Policy
    UNSUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.policy.toString(), "GET Bucket policy");

    // Notification
    UNSUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.notification.toString(),
        "GET Bucket notification");

    // Request Payments // TODO HACK! binding code converts parameters to lower case. Fix that issue!
    UNSUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.requestPayment.toString().toLowerCase(),
        "GET Bucket requestPayment");

    // Website
    UNSUPPORTED_OPS.put(BUCKET + HttpMethod.GET.toString() + ObjectStorageProperties.BucketParameter.website.toString(), "GET Bucket website");
  }

  @Override
  protected Map<String, String> populateOperationMap() {
    return SUPPORTED_OPS;
  }

  @Override
  protected Map<String, String> populateUnsupportedOperationMap() {
    return UNSUPPORTED_OPS;
  }
}
