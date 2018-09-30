/**
 * Copyright 2018 AppScale Systems, Inc
 *
 * Use of this source code is governed by a BSD-2-Clause
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/BSD-2-Clause
 */
package com.eucalyptus.objectstorage.providers.s3;

import com.eucalyptus.objectstorage.providers.ObjectStorageProviders.ObjectStorageProviderClientProperty;

/**
 * Implementation for Minio. Add any additional Minio specific behavior here.
 */
@ObjectStorageProviderClientProperty("minio")
public class MinioProviderClient extends S3ProviderClient {
}