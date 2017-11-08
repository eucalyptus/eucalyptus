/*************************************************************************
 * Copyright 2016 Ent. Services Development Corporation LP
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
package com.eucalyptus.portal.provider.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
import com.eucalyptus.objectstorage.policy.S3PolicySpec;
import com.eucalyptus.portal.common.provider.TagProvider;
import com.google.common.collect.Sets;

/**
 * Tag provider for S3 tags
 */
public class S3TagProvider implements TagProvider {
  private static final Logger logger = Logger.getLogger( S3TagProvider.class );

  @Nonnull
  @Override
  public String getVendor( ) {
    return S3PolicySpec.VENDOR_S3;
  }

  @Nonnull
  @Override
  public Set<String> getTagKeys(
      @Nonnull final User user
  ) {
    final Set<String> tagKeys = Sets.newHashSet( );
    try ( final EucaS3Client client = EucaS3ClientFactory.getEucaS3Client( user ) ) {
      for ( final Bucket bucket : client.listBuckets( ) ) try {
        final BucketTaggingConfiguration taggingConfiguration =
            client.getBucketTaggingConfiguration( bucket.getName( ) );
        if ( taggingConfiguration != null ) {
          tagKeys.addAll( taggingConfiguration.getTagSet( ).getAllTags( ).keySet( ) );
        }
      } catch ( final AmazonServiceException e ) {
        if ( !"NoSuchTagSetError".equals( e.getErrorCode( ) ) ) {
          throw e;
        }
      }
    } catch ( final Exception e ) {
      logger.error( "Error describing keys for s3", e );
    }
    return tagKeys;
  }
}
