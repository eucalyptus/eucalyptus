/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package com.eucalyptus.util;

//import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;
import org.apache.log4j.Logger;

import com.eucalyptus.util.BaseDirectory;

import java.net.*;
import java.util.List;
import java.util.Collections;

public class WalrusProperties {
	private static Logger LOG = Logger.getLogger( WalrusProperties.class );

	public static final String SERVICE_NAME = "Walrus";
	public static final String VIRTUAL_SUBDOMAIN = "I_R_Bukkit";
	public static String WALRUS_SUBDOMAIN = "walrus";
	public static String WALRUS_IP = "127.0.0.1";
	public static final long G = 1024*1024*1024;
	public static final long M = 1024*1024;
	public static final long K = 1024;

	public static String bucketRootDirectory = BaseDirectory.VAR.toString() + "/bukkits";
	public static int MAX_BUCKETS_PER_USER = 5;
	public static long MAX_BUCKET_SIZE = 5 * G;
	public static long IMAGE_CACHE_SIZE = 30 * G;
	public static String WALRUS_URL;
	public static int MAX_TOTAL_SNAPSHOT_SIZE = 50;
	public static int MAX_KEYS = 1000;

	public static final int IO_CHUNK_SIZE = 102400;
	public static boolean shouldEnforceUsageLimits = true;
	public static boolean trackUsageStatistics = true;
	public static boolean enableSnapshots = false;
	public static boolean enableTorrents = false;
	public static boolean sharedMode = false;
	public static boolean enableVirtualHosting = true;
	public static final long CACHE_PROGRESS_TIMEOUT = 600000L; //ten minutes
	public static long IMAGE_CACHE_RETRY_TIMEOUT = 1000L;
	public static final int IMAGE_CACHE_RETRY_LIMIT = 3;

	public static final String walrusServicePath = "/services/Walrus";
	public static final String NAMESPACE_VERSION = "2006-03-01";
	public static final String CONTENT_LEN = "Content-Length";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String CONTENT_DISPOSITION = "Content-Disposition";
	public static final String MULTIFORM_DATA_TYPE = "multipart/form-data";
	public static void update() {
		//TODO
		/*try {
            SystemConfiguration systemConfiguration = EucalyptusProperties.getSystemConfiguration();
            bucketRootDirectory = systemConfiguration.getStorageDir();
            MAX_BUCKETS_PER_USER = systemConfiguration.getStorageMaxBucketsPerUser();
            MAX_BUCKET_SIZE = systemConfiguration.getStorageMaxBucketSizeInMB() * M;
            IMAGE_CACHE_SIZE = systemConfiguration.getStorageMaxCacheSizeInMB() * M;
            WALRUS_URL = systemConfiguration.getStorageUrl();
            java.net.URI walrusAddrUri = new URL(WALRUS_URL).toURI();
            TRACKER_URL = "http://" + walrusAddrUri.getHost() + ":" + TRACKER_PORT + "/announce";
            Integer maxTotalSnapSize = systemConfiguration.getStorageMaxTotalSnapshotSizeInGb();
            if(maxTotalSnapSize != null) {
                if(maxTotalSnapSize > 0) {
                    MAX_TOTAL_SNAPSHOT_SIZE = maxTotalSnapSize;
                }
            }
            UpdateWalrusConfigurationType updateConfig = new UpdateWalrusConfigurationType();
            updateConfig.setBucketRootDirectory(bucketRootDirectory);
            Messaging.send( WALRUS_REF, updateConfig );
        } catch(Exception ex) {
            LOG.warn(ex.getMessage());
        }*/
	}

	public static final String URL_PROPERTY = "euca.walrus.url";
	public static final String USAGE_LIMITS_PROPERTY = "euca.walrus.usageLimits";
	public static final String WALRUS_OPERATION = "WalrusOperation";
	public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";
	public static final String STREAMING_HTTP_GET = "STREAMING_HTTP_GET";
	public static final String STREAMING_HTTP_PUT = "STREAMING_HTTP_PUT";
	public static final String AMZ_ACL = "x-amz-acl";
	public static final String ALL_USERS_GROUP = "http://acs.amazonaws.com/groups/global/AllUsers";
	public static final String AUTHENTICATED_USERS_GROUP = "'http://acs.amazonaws.com/groups/global/AuthenticatedUsers";

	public static final String IGNORE_PREFIX = "x-ignore-";
	public static final String COPY_SOURCE = "x-amz-copy-source";
	public static final String METADATA_DIRECTIVE = "x-amz-metadata-directive";
	public static final String ADMIN = "admin";
	public static String WALRUS_REF = "vm://BukkitInternal";

	public static String TRACKER_BINARY_DIR = "/usr/bin";
	public static String TRACKER_BINARY = TRACKER_BINARY_DIR + "/bttrack";
	public static String TORRENT_CREATOR_BINARY = TRACKER_BINARY_DIR + "/btmakemetafile";
	public static String TORRENT_CLIENT_BINARY = TRACKER_BINARY_DIR + "/btdownloadheadless";
	public static String TRACKER_DIR = BaseDirectory.VAR.toString() + "/bt";
	public static String TRACKER_URL = "http://localhost:6969/announce";
	public static String TRACKER_PORT = "6969";

	public enum Headers {
		Bucket, Key, RandomKey, VolumeId
	}

	public enum ExtendedGetHeaders {
		IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
	}

	public enum ExtendedHeaderDateTypes {
		IfModifiedSince, IfUnmodifiedSince, CopySourceIfModifiedSince, CopySourceIfUnmodifiedSince;

		public static boolean contains(String value) {
			for(ExtendedHeaderDateTypes type: values()) {
				if(type.toString().equals(value)) {
					return true;
				}
			}
			return false;
		}
	}

	public enum ExtendedHeaderRangeTypes {
		ByteRangeStart, ByteRangeEnd
	}

	public enum WalrusInternalOperations {
		GetDecryptedImage
	}

	public enum GetOptionalParameters {
		IsCompressed
	}

	public enum StorageOperations {
		StoreSnapshot, DeleteWalrusSnapshot, GetWalrusSnapshot
	}

	public enum InfoOperations {
		GetSnapshotInfo
	}

	public enum StorageParameters {
		SnapshotVgName, SnapshotLvName
	}

	public enum FormField {
		FormUploadPolicyData, AWSAccessKeyId, key, bucket, acl, policy, success_action_redirect, success_action_status, signature, file
	}

	public enum IgnoredFields {
		AWSAccessKeyId, signature, file, policy, submit
	}

	public enum PolicyHeaders {
		expiration, conditions
	}

	public enum CopyHeaders {
		CopySourceIfMatch, CopySourceIfNoneMatch, CopySourceIfUnmodifiedSince, CopySourceIfModifiedSince
	}

	public enum SubResource {
		acl, logging, torrent, location
	}

	public enum HTTPVerb {
		GET, PUT, DELETE, POST, HEAD;
	}

	public enum OperationParameter {
		acl, location, prefix, maxkeys, delimiter, marker, torrent, logging;
	}
	
    public enum RequiredQueryParams {
        Date
    }
    
    public enum RequiredSOAPTags {
    	AWSAccessKeyId, Timestamp, Signature
    }
}
