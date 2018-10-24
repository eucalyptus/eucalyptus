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
package com.eucalyptus.compute.common.internal.tags;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import com.eucalyptus.auth.principal.AccountFullName;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableMap;
import io.vavr.Tuple;
import io.vavr.Tuple2;

/**
 *
 */
public class TagCache {

  private static final String CACHE_SPEC =
      System.getProperty( "com.eucalyptus.compute.tags.cacheSpec", "maximumSize=1000, expireAfterWrite=10s" );

  private static final TagCache INSTANCE = new TagCache( );

  private Cache<Tuple2<String,String>,Map<String,String>> cache =
      CacheBuilder.from( CacheBuilderSpec.parse( CACHE_SPEC ) ).build( );

  public static TagCache getInstance( ) {
    return INSTANCE;
  }

  public Map<String,String> getTagsForResource(
      final String resourceId,
      final String accountNumber
  ) {
    try {
      return cache.get( Tuple.of( accountNumber, resourceId), () -> {
        Map<String,String> tags = Collections.emptyMap( );
        final TagSupport tagSupport = TagSupport.fromIdentifier( resourceId );
        if ( tagSupport != null ) {
          final Map<String,List<Tag>> tagsByResource =
              tagSupport.getResourceTagMap(
                  AccountFullName.getInstance( accountNumber ),
                  Collections.singleton( resourceId ) );
          final List<Tag> tagList = tagsByResource.get( resourceId );
          if ( tagList != null ) {
            tags = ImmutableMap.copyOf(
                tagList.stream( ).collect( Collectors.toMap( Tag::getKey, Tag::getValue ) ) );
          }
        }
        return tags;
      } );
    } catch ( final ExecutionException e ) {
      return Collections.emptyMap( );
    }
  }
}
