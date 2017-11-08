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
