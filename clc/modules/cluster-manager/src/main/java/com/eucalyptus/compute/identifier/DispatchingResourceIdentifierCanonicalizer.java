/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/
package com.eucalyptus.compute.identifier;

import javax.annotation.Nullable;
import com.eucalyptus.util.CollectionUtils;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

/**
 *
 */
class DispatchingResourceIdentifierCanonicalizer implements ResourceIdentifierCanonicalizer {

  private final Iterable<ResourceIdentifierCanonicalizer> canonicalizers;

  DispatchingResourceIdentifierCanonicalizer( final Iterable<ResourceIdentifierCanonicalizer> canonicalizers ) {
    this.canonicalizers = canonicalizers;
  }

  @Override
  public String getName() {
    return Joiner.on(",").join( Iterables.transform(
        canonicalizers,
        ResourceIdentifiers.ResourceIdentifierCanonicalizerToName.INSTANCE ) );
  }

  @Override
  public String canonicalizePrefix( final String prefix ) {
    return CollectionUtils.reduce( canonicalizers, prefix, new Function<String,Function<ResourceIdentifierCanonicalizer,String>>( ){
      @Nullable
      @Override
      public Function<ResourceIdentifierCanonicalizer, String> apply( final String value ) {
        return new Function<ResourceIdentifierCanonicalizer,String>( ){
          @Nullable
          @Override
          public String apply( final ResourceIdentifierCanonicalizer canonicalizer ) {
            return canonicalizer.canonicalizePrefix( value );
          }
        };
      }
    } );
  }

  @Override
  public String canonicalizeHex( final String hex ) {
    return CollectionUtils.reduce( canonicalizers, hex, new Function<String,Function<ResourceIdentifierCanonicalizer,String>>( ){
      @Nullable
      @Override
      public Function<ResourceIdentifierCanonicalizer, String> apply( final String value ) {
        return new Function<ResourceIdentifierCanonicalizer,String>( ){
          @Nullable
          @Override
          public String apply( final ResourceIdentifierCanonicalizer canonicalizer ) {
            return canonicalizer.canonicalizeHex( value );
          }
        };
      }
    } );
  }
}
