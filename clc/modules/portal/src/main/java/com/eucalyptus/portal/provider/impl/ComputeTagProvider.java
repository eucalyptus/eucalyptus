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
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.compute.common.Compute;
import com.eucalyptus.compute.common.DescribeTagsResponseType;
import com.eucalyptus.compute.common.DescribeTagsType;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.portal.common.provider.TagProvider;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Sets;

/**
 * Tag provider for EC2 tags.
 */
public class ComputeTagProvider implements TagProvider {

  private static final Logger logger = Logger.getLogger( ComputeTagProvider.class );

  @Nonnull
  @Override
  public String getVendor( ) {
    return PolicySpec.VENDOR_EC2;
  }

  @Nonnull
  @Override
  public Set<String> getTagKeys(
      @Nonnull final User user
  ) {
    final Set<String> tagKeys = Sets.newHashSet( );
    try {
      final DescribeTagsType describeTags = new DescribeTagsType( );
      describeTags.setUserId( user.getUserId( ) );
      describeTags.markPrivileged( );
      final DescribeTagsResponseType response = AsyncRequests.sendSync( Compute.class, describeTags );
      tagKeys.addAll( response.getTagSet( ).stream( ).map( TagInfo::getKey ).collect( Collectors.toSet( ) ) );
    } catch ( Exception e ) {
      logger.error( "Error describing keys for ec2", e );
    }
    return tagKeys;
  }
}
