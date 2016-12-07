/*************************************************************************
 * (c) Copyright 2016 Hewlett Packard Enterprise Development Company LP
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
package com.eucalyptus.portal.provider.impl;

import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.objectstorage.client.EucaS3Client;
import com.eucalyptus.objectstorage.client.EucaS3ClientFactory;
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
    return PolicySpec.VENDOR_S3;
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
