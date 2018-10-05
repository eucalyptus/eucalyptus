/*************************************************************************
 * Copyright 2008 Regents of the University of California
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights
 *   Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *   or in the "license" file accompanying this file. This file is
 *   distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 *   ANY KIND, either express or implied. See the License for the specific
 *   language governing permissions and limitations under the License.
 ************************************************************************/

package com.eucalyptus.objectstorage.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.handler.codec.http.HttpHeaders;

import com.eucalyptus.component.Topology;
import com.eucalyptus.objectstorage.ObjectStorage;
import com.eucalyptus.system.BaseDirectory;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ObjectStorageProperties {
  private static Logger LOG = Logger.getLogger(ObjectStorageProperties.class);

  public static final long G = 1024 * 1024 * 1024;
  public static final long M = 1024 * 1024;
  public static final long K = 1024;

  public static int IO_CHUNK_SIZE = 4096;
  public static boolean enableTorrents = false;
  public static final String NAMESPACE_VERSION = "2006-03-01";
  public static int MAX_KEYS = 1000;
  public static int MIN_PART_NUMBER = 1;
  public static int MAX_PART_NUMBER = 10000;

  public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";

  public static final String AMZ_ACL = "x-amz-acl";
  public static final String AMZ_REQUEST_ID = "x-amz-request-id";
  public static final String LEGACY_WALRUS_SERVICE_PATH = "/services/Walrus";
  public static final long OBJECT_CREATION_EXPIRATION_INTERVAL_SEC = 30;

  // TODO: zhill - these should be replaced by references to the actual Accounts lookup. May need to add these as valid groups/accounts
  public static enum S3_GROUP {
    ALL_USERS_GROUP {
      public String toString() {
        return "http://acs.amazonaws.com/groups/global/AllUsers";
      }
    },
    AUTHENTICATED_USERS_GROUP {
      public String toString() {
        return "http://acs.amazonaws.com/groups/global/AuthenticatedUsers";
      }
    },
    LOGGING_GROUP {
      public String toString() {
        return "http://acs.amazonaws.com/groups/s3/LogDelivery";
      }
    },

    // TODO: this is wrong. za-team is a user, not a group. Should use canonicalId: 6aa5a366c34c1cbe25dc49211496e913e0351eb0e8c37aa3477e40942ec6b97c
    AWS_EXEC_READ {
      public String toString() {
        return "http://acs.amazonaws.com/groups/s3/zateam";
      }
    }, // Used for the system for vm images

    // This is a made-up group in place of the ec2-bundled-image
    EC2_BUNDLE_READ {
      public String toString() {
        return "http://acs.amazonaws.com/groups/s3/ec2-bundle";
      }
    } // Used for the system for vm images

  }

  public static final String IGNORE_PREFIX = "x-ignore-";
  public static final String COPY_SOURCE = "x-amz-copy-source";
  public static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";

  public static final String X_AMZ_VERSION_ID = "x-amz-version-id";
  public static final String NULL_VERSION_ID = "null";
  public static final String X_AMZ_DELETE_MARKER = "x-amz-delete-marker";

  public static final String TRACKER_BINARY = "bttrack";
  public static final String TORRENT_CREATOR_BINARY = "btmakemetafile";
  public static final String TORRENT_CLIENT_BINARY = "btdownloadheadless";
  public static String TRACKER_DIR = BaseDirectory.VAR.toString() + "/bt";
  public static String TRACKER_URL = "http://localhost:6969/announce";
  public static String TRACKER_PORT = "6969";

  public static long MAX_INLINE_DATA_SIZE = 10 * M;
  // public static String FORM_BOUNDARY_FIELD = IGNORE_PREFIX + "euca-form-boundary"; //internal header value for passing info
  // public static String FIRST_CHUNK_FIELD = IGNORE_PREFIX + "FirstDataChunk";
  // public static String UPLOAD_LENGTH_FIELD = IGNORE_PREFIX + "FileContentLength";
  public static long MPU_PART_MIN_SIZE = 5 * 1024 * 1024; // 5MB

  // 15 minutes
  public final static long EXPIRATION_LIMIT = 900000;

  public static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("^bytes=(\\d*)-(\\d*)$");
  public static final Pattern GRANT_HEADER_PATTERN = Pattern
      .compile("^(\\s*(emailAddress|id|uri)=['\"]?[^,'\"]+['\"]?\\s*)(,\\s*(emailAddress|id|uri)=['\"]?[^,'\"]+['\"]?\\s*)*$");

  public enum CannedACL {
    private_only {
      public String toString() {
        return "private";
      }
    },
    public_read {
      public String toString() {
        return "public-read";
      }
    },
    public_read_write {
      public String toString() {
        return "public-read-write";
      }
    },
    authenticated_read {
      public String toString() {
        return "authenticated-read";
      }
    },
    bucket_owner_read {
      public String toString() {
        return "bucket-owner-read";
      }
    },
    bucket_owner_full_control {
      public String toString() {
        return "bucket-owner-full-control";
      }
    },
    log_delivery_write {
      public String toString() {
        return "log-delivery-write";
      }
    },
    aws_exec_read {
      public String toString() {
        return "aws-exec-read";
      }
    },
    ec2_bundle_read {
      public String toString() {
        return "ec2-bundle-read";
      }
    }
  }

  public enum STORAGE_CLASS {
    STANDARD, REDUCED_REDUNDANCY;
  }

  public enum Permission {
    READ, WRITE, READ_ACP, WRITE_ACP, FULL_CONTROL
  }

  public enum Resource {
    bucket, object
  }

  public enum X_AMZ_GRANT {
    READ("x-amz-grant-read"), WRITE("x-amz-grant-write"), READ_ACP("x-amz-grant-read-acp"), WRITE_ACP("x-amz-grant-write-acp"), FULL_CONTROL(
        "x-amz-grant-full-control");

    private final String header;

    private X_AMZ_GRANT(String header) {
      this.header = header;
    }

    @Override
    public String toString() {
      return this.header;
    }
  }

  public static final Map<X_AMZ_GRANT, Permission> HEADER_PERMISSION_MAP = ImmutableMap.<X_AMZ_GRANT, Permission>builder()
      .put(X_AMZ_GRANT.READ, Permission.READ).put(X_AMZ_GRANT.WRITE, Permission.WRITE).put(X_AMZ_GRANT.READ_ACP, Permission.READ_ACP)
      .put(X_AMZ_GRANT.WRITE_ACP, Permission.WRITE_ACP).put(X_AMZ_GRANT.FULL_CONTROL, Permission.FULL_CONTROL).build();

  /**
   * Splitting function for parsing x-amz-grant-* headers into a list of [key,value] string arrays</p>
   * 
   * Input to the function, header value: <code>x-amz-grant-read: emailAddress="xyz@amazon.com", uri="http://some-uri",id="canonical-id"</code></p>
   * 
   * Output from the function, list of string arrays: <code>{[emailAddress, xyz@amazon.com],[uri, http://some-uri],[id, canonical-id]}</code> </p>
   */
  // Tried using MapSplitter which does the same thing. However, it does not allow duplicate keys in the key-value output. So if the grant has two or
  // more of the same identity elements (uri, id, emailAddress), only one of them gets through
  public static final Function<String, List<String[]>> GRANT_HEADER_PARSER = new Function<String, List<String[]>>() {

    private final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();
    private final Splitter KEY_VALUE_SPLITTER = Splitter.on('=').limit(2).omitEmptyStrings()
        .trimResults(CharMatcher.anyOf("'\"").or(CharMatcher.WHITESPACE));

    @Override
    public List<String[]> apply(String arg0) {
      List<String[]> returnvalue = Lists.newArrayList();

      for (String gpString : COMMA_SPLITTER.split(arg0)) {
        Iterator<String> gpIterator = KEY_VALUE_SPLITTER.split(gpString).iterator();
        String idType = null;
        String idValue = null;

        if (gpIterator != null && gpIterator.hasNext()) {
          idType = gpIterator.next();
        } else {
          continue; // drop the header
        }

        if (gpIterator.hasNext()) {
          idValue = gpIterator.next();
        } else {
          continue; // drop the header
        }

        if (StringUtils.isNotBlank(idType) && StringUtils.isNotBlank(idValue)) {
          returnvalue.add(new String[] {idType, idValue});
        } else {
          continue; // drop the header
        }
      }

      return returnvalue;
    }
  };

  public enum VersioningStatus {
    Enabled, Disabled, Suspended
  }

  public enum Headers {
    Bucket, Key, RandomKey, VolumeId, S3UploadPolicy, S3UploadPolicySignature
  }

  public enum SubResource {
    // Per the S3 Dev guide, these must be included in the canonicalized resource:
    acl(true), lifecycle, location, logging, notification, partNumber, policy, requestPayment, torrent(true), uploadId, uploads, versionId, versioning, versions, website, cors, tagging, delete;

    /** Indicates whether the SubResource is of this <a href="http://docs.aws.amazon.com/AmazonS3/latest/dev/ObjectAndSoubResource.html">type</a> */
    public final boolean isObjectSubResource;

    SubResource(){
      isObjectSubResource = false;
    }

    SubResource(boolean isObjectSubResource) {
      this.isObjectSubResource = isObjectSubResource;
    }
  }

  public enum ExtendedGetHeaders {
    IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
  }

  public enum ExtendedHeaderDateTypes {
    IfModifiedSince, IfUnmodifiedSince, CopySourceIfModifiedSince, CopySourceIfUnmodifiedSince;

    public static boolean contains(String value) {
      for (ExtendedHeaderDateTypes type : values()) {
        if (type.toString().equals(value)) {
          return true;
        }
      }
      return false;
    }
  }

  public enum ExtendedHeaderRangeTypes {
    ByteRangeStart, ByteRangeEnd
  }

  public enum GetOptionalParameters {
    IsCompressed
  }

  public enum FormField {
    AWSAccessKeyId, key, bucket, acl, Policy, redirect, success_action_redirect, success_action_status, x_amz_security_token {
      public String toString() {
        return "x-amz-security-token";
      }
    },
    Signature, file, Cache_Control {
      public String toString() {
        return HttpHeaders.Names.CACHE_CONTROL;
      }
    },
    Content_Type {
      public String toString() {
        return HttpHeaders.Names.CONTENT_TYPE;
      }
    },
    Content_Disposition {
      public String toString() {
        return "Content-Disposition";
      }
    },
    Content_Encoding {
      public String toString() {
        return HttpHeaders.Names.CONTENT_ENCODING;
      }
    },
    Expires, x_ignore_firstdatachunk {
      public String toString() {
        return "x-ignore-firstdatachunk";
      }
    },
    x_ignore_filecontentlength {
      public String toString() {
        return "x-ignore-filecontentlength";
      }
    },
    x_ignore_formboundary {
      public String toString() {
        return "x-ignore-formboundary";
      }
    };

    private static Set<FormField> HTTPHeaderFields = Sets.newHashSet(FormField.Content_Type, FormField.Cache_Control, FormField.Content_Disposition,
        FormField.Content_Encoding, FormField.Expires);

    public static boolean isHttpField(String value) {
      try {
        return HTTPHeaderFields.contains(FormField.valueOf(value.replace('-', '_')));
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
  }

  public enum IgnoredFields {
    AWSAccessKeyId, Signature, file, Policy, submit
  }

  public enum PolicyHeaders {
    expiration, conditions
  }

  public enum CopyHeaders {
    CopySourceIfMatch, CopySourceIfNoneMatch, CopySourceIfUnmodifiedSince, CopySourceIfModifiedSince
  }

  public enum MetadataDirective {
    COPY, REPLACE
  }

  public enum ResponseHeaderOverrides {
    response_content_type {
      public String toString() {
        return "response-content-type";
      }
    },
    response_content_language {
      public String toString() {
        return "response-content-language";
      }
    },
    response_expires {
      public String toString() {
        return "response-expires";
      }
    },
    response_cache_control {
      public String toString() {
        return "response-cache-control";
      }
    },
    response_content_disposition {
      public String toString() {
        return "response-content-disposition";
      }
    },
    response_content_encoding {
      public String toString() {
        return "response-content-encoding";
      }
    };

    public static ResponseHeaderOverrides fromString(String value) {
      if (value != null && "response-content-type".equals(value)) {
        return response_content_type;
      }
      if (value != null && "response-content-language".equals(value)) {
        return response_content_language;
      }
      if (value != null && "response-expires".equals(value)) {
        return response_expires;
      }
      if (value != null && "response-cache-control".equals(value)) {
        return response_cache_control;
      }
      if (value != null && "response-content-disposition".equals(value)) {
        return response_content_disposition;
      }
      if (value != null && "response-content-encoding".equals(value)) {
        return response_content_encoding;
      }

      return null;
    }
  }

  public static final Map<String, String> RESPONSE_OVERRIDE_HTTP_HEADER_MAP = ImmutableMap.<String, String>builder()
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_content_type.toString(), HttpHeaders.Names.CONTENT_TYPE)
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_content_language.toString(), HttpHeaders.Names.CONTENT_LANGUAGE)
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_expires.toString(), HttpHeaders.Names.EXPIRES)
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_cache_control.toString(), HttpHeaders.Names.CACHE_CONTROL)
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_content_disposition.toString(), "Content-Disposition")
      .put(ObjectStorageProperties.ResponseHeaderOverrides.response_content_encoding.toString(), HttpHeaders.Names.CONTENT_ENCODING).build();

  public enum HTTPVerb {
    GET, PUT, DELETE, POST, HEAD, OPTIONS;
  }

  public enum ServiceParameter {
  }

  public enum BucketParameter {
    acl, location, prefix, maxkeys, delimiter, marker, torrent, logging, versioning, versions, versionidmarker, keymarker, cors, lifecycle, policy, notification, tagging, requestPayment, website, uploads, maxUploads, uploadIdMarker, delete, startafter, continuationtoken, fetchowner, listtype
  }

  public enum ObjectParameter {
    acl, torrent, versionId, uploads, partNumber, uploadId, maxParts, partNumberMarker;
  }

  public enum RequiredQueryParams {
    Date
  }

  public static String getTrackerUrl() {
    try {
      TRACKER_URL = "http://" + Topology.lookup(ObjectStorage.class).getUri().getHost() + ":" + TRACKER_PORT + "/announce";
    } catch (Exception e) {
      LOG.error(e);
    }
    return TRACKER_URL;
  }
}
