/*************************************************************************
 * Copyright 2017 Ent. Services Development Corporation LP
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
 * POSSIBILITY OF SUCH DAMAGE.
 ************************************************************************/
package com.eucalyptus.objectstorage.pipeline.binding;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import com.eucalyptus.objectstorage.msgs.GetBucketPolicyResponseType;
import com.eucalyptus.objectstorage.msgs.ObjectStorageResponseType;
import io.vavr.Tuple;
import io.vavr.Tuple2;

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
