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

package edu.ucsb.eucalyptus.util;

import org.apache.log4j.Logger;
import edu.ucsb.eucalyptus.cloud.entities.SystemConfiguration;
import edu.ucsb.eucalyptus.msgs.UpdateWalrusConfigurationType;

public class WalrusProperties {
    private static Logger LOG = Logger.getLogger( WalrusProperties.class );

    public static final String SERVICE_NAME = "Walrus";
    public static final long G = 1024*1024*1024;
    public static final long M = 1024*1024;
    public static final long K = 1024;

    public static String bucketRootDirectory = BaseDirectory.VAR.toString() + "/bukkits";
    public static int MAX_BUCKETS_PER_USER = 5;
    public static long MAX_BUCKET_SIZE = 5 * G;
    public static long IMAGE_CACHE_SIZE = 30 * G;

    public static void update() {
        try {
            SystemConfiguration systemConfiguration = EucalyptusProperties.getSystemConfiguration();
            bucketRootDirectory = systemConfiguration.getStorageDir();
            MAX_BUCKETS_PER_USER = systemConfiguration.getStorageMaxBucketsPerUser();
            MAX_BUCKET_SIZE = systemConfiguration.getStorageMaxBucketSizeInMB() * M;
            IMAGE_CACHE_SIZE = systemConfiguration.getStorageMaxCacheSizeInMB() * M;
            UpdateWalrusConfigurationType updateConfig = new UpdateWalrusConfigurationType();
            updateConfig.setBucketRootDirectory(bucketRootDirectory);
            Messaging.send( WALRUS_REF, updateConfig );
        } catch(Exception ex) {
            LOG.warn(ex.getMessage());
        }
    }

    static {
        update();
    }

    public static final String URL_PROPERTY = "euca.walrus.url";
    public static final String USAGE_LIMITS_PROPERTY = "euca.walrus.usageLimits";
    public static final String WALRUS_OPERATION = "WalrusOperation";
    public static final String AMZ_META_HEADER_PREFIX = "x-amz-meta-";
    public static final String STREAMING_HTTP_GET = "STREAMING_HTTP_GET";
    public static final String STREAMING_HTTP_PUT = "STREAMING_HTTP_PUT";
    public static final String AMZ_ACL = "x-amz-acl";
    public static final String ADMIN = "admin";
    public static String WALRUS_REF = "vm://BukkitInternal";

    public enum Headers {
        Bucket, Key, RandomKey, VolumeId
    }

    public enum ExtendedGetHeaders {
        IfModifiedSince, IfUnmodifiedSince, IfMatch, IfNoneMatch, Range
    }

    public enum ExtendedHeaderDateTypes {
        IfModifiedSince, IfUnmodifiedSince;

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
}