/*************************************************************************
 * (c) Copyright 2017 Hewlett Packard Enterprise Development Company LP
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 ************************************************************************/
package com.eucalyptus.objectstorage.pipeline.binding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.eucalyptus.objectstorage.msgs.GetBucketPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import javaslang.Tuple;
import javaslang.Tuple2;

/**
 *
 */
public class BucketPolicyS3ResponseEncoder implements S3ResponseEncoder {

  @Override
  public boolean canEncode( final ObjectStorageResponseType response ) {
    return response instanceof GetBucketPolicyResponseType;
  }

  @Override
  public Tuple2<String,byte[]> encode( final ObjectStorageResponseType response ) throws IOException {
    if ( !( response instanceof GetBucketPolicyResponseType ) ) {
      throw new IOException( "Cannot encode response of type " + response.getClass( ) );
    }
    return Tuple.of(
        "text/plain",
        ( (GetBucketPolicyResponseType) response ).getPolicy( ).getBytes( StandardCharsets.UTF_8 )
    );
  }
}
