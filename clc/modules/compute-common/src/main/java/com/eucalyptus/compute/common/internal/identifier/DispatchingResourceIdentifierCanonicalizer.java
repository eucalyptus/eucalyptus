/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
package com.eucalyptus.compute.common.internal.identifier;

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
