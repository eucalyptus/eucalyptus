/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.providers.s3;

import com.eucalyptus.objectstorage.providers.ObjectStorageProviders.ObjectStorageProviderClientProperty;

/**
 * Implementation for Minio. Add any additional Minio specific behavior here.
 */
@ObjectStorageProviderClientProperty("minio")
public class MinioProviderClient extends S3ProviderClient {
}