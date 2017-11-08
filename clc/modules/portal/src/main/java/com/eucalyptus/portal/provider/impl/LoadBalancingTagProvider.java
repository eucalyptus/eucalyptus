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
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.loadbalancing.common.LoadBalancing;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsResponseType;
import com.eucalyptus.loadbalancing.common.msgs.DescribeTagsType;
import com.eucalyptus.loadbalancing.common.msgs.Tag;
import com.eucalyptus.loadbalancing.common.policy.LoadBalancingPolicySpec;
import com.eucalyptus.portal.common.provider.TagProvider;
import com.eucalyptus.util.async.AsyncRequests;
import com.google.common.collect.Sets;

/**
 * Tag provider for ELB tags
 */
public class LoadBalancingTagProvider implements TagProvider {
  private static final Logger logger = Logger.getLogger( LoadBalancingTagProvider.class );

  @Nonnull
  @Override
  public String getVendor( ) {
    return LoadBalancingPolicySpec.VENDOR_LOADBALANCING;
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
      final DescribeTagsResponseType response = AsyncRequests.sendSync( LoadBalancing.class, describeTags );
      tagKeys.addAll( response.getDescribeTagsResult( ).getTagDescriptions( ).getMember( ).stream( )
          .flatMap( tagDesc -> tagDesc.getTags( ).getMember( ).stream( ) )
          .map( Tag::getKey ).collect( Collectors.toSet( ) ) );
    } catch ( Exception e ) {
      logger.error( "Error describing keys for elb", e );
    }
    return tagKeys;
  }
}
