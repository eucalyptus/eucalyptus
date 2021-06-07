/**
 * Copyright 2019 AppScale Systems, Inc
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */
package com.eucalyptus.objectstorage.msgs;

import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.Nullable;
import com.eucalyptus.storage.msgs.s3.MetaDataEntry;

/**
 * Implemented by requests that create objects for common metadata processing.
 */
public interface ObjectMetadataRequestType {

  @Nullable
  default String getContentDisposition( ) { return null; };

  @Nullable
  default String getContentLength( ) { return null; }

  @Nullable
  default String getContentMD5( ) { return null; }

  @Nullable
  String getContentType( );

  @Nullable
  default HashMap<String, String> getCopiedHeaders( ) { return null; };

  @Nullable
  ArrayList<MetaDataEntry> getMetaData( );

}
