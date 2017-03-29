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
import javaslang.Tuple;
import javaslang.Tuple2;

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
