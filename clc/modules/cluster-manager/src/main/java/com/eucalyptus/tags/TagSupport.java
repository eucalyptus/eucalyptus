/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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
package com.eucalyptus.tags;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

/**
 * Support functionality for each resource that supports tagging
 */
public abstract class TagSupport {

  private static final ConcurrentMap<String,TagSupport> supportByIdentifierPrefix = Maps.newConcurrentMap();
  private static final ConcurrentMap<Class<? extends CloudMetadata>,TagSupport> supportByClass = Maps.newConcurrentMap();
  private static final Splitter idSplitter = Splitter.on( '-' ).limit( 2 );
  private static final ConcurrentMap<Class,Class> metadataClassMap = new MapMaker().makeComputingMap( MetadataSubclass.INSTANCE );
  
  private final Class<? extends CloudMetadata> cloudMetadataClass;
  private final Set<String> identifierPrefixes;

  protected TagSupport( @Nonnull final Class<? extends CloudMetadata> cloudMetadataClass,
                        @Nonnull final Set<String> identifierPrefixes ) {
    this.cloudMetadataClass = cloudMetadataClass;
    this.identifierPrefixes = ImmutableSet.copyOf( identifierPrefixes );
  }

  protected TagSupport( @Nonnull final Class<? extends CloudMetadata> cloudMetadataClass,
                        @Nonnull final String identifierPrefix ) {
    this( cloudMetadataClass, Collections.singleton( identifierPrefix ) );
  }

  public abstract Tag createOrUpdate( CloudMetadata metadata, 
                                      OwnerFullName ownerFullName,
                                      String key, 
                                      String value );
  
  public abstract Tag example( @Nonnull CloudMetadata metadata,
                               @Nonnull OwnerFullName ownerFullName,
                               @Nullable String key,
                               @Nullable String value );

  public abstract CloudMetadata lookup( String identifier ) throws TransactionException;

  @Nonnull
  public Class<? extends CloudMetadata> getCloudMetadataClass() {
    return cloudMetadataClass;
  }

  @Nonnull
  public Set<String> getIdentifierPrefixes() {
    return identifierPrefixes;
  }

  public static TagSupport fromResource( @Nonnull final CloudMetadata metadata ) {
    return supportByClass.get( subclassFor( metadata.getClass() ) );
  }

  public static TagSupport fromIdentifier( @Nonnull final String id ) {
    return supportByIdentifierPrefix.get( Iterables.getFirst( idSplitter.split( id ), "" ) );
  }
  
  static void registerTagSupport( @Nonnull final TagSupport tagSupport ) {
    supportByClass.put( tagSupport.getCloudMetadataClass(), tagSupport );
    for ( final String idPrefix : tagSupport.getIdentifierPrefixes() ) {
      supportByIdentifierPrefix.put( idPrefix, tagSupport ); 
    }
  }

  @SuppressWarnings( "unchecked" )
  private static Class<? extends CloudMetadata> subclassFor( Class<? extends CloudMetadata> metadataInstance ) {
    return metadataClassMap.get( metadataInstance );  
  }

  private enum MetadataSubclass implements Function<Class,Class> {
    INSTANCE;

    @Override
    public Class apply( final Class instanceClass ) {
      final List<Class<?>> interfaces = Lists.newArrayList();
      for ( final Class clazz : Classes.interfaceAncestors().apply( instanceClass ) ) {
        interfaces.add( clazz );
      }
      Collections.reverse( interfaces );
      return Iterables.find( interfaces,
          Predicates.and(
              Predicates.not( Predicates.<Class<?>>equalTo( CloudMetadata.class ) ),
              Classes.subclassOf( CloudMetadata.class ) ) );
    }
  }
}
