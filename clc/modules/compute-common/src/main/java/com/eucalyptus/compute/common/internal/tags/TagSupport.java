/*************************************************************************
 * Copyright 2009-2013 Ent. Services Development Corporation LP
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
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.Classes;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

/**
 * Support functionality for each resource that supports tagging
 */
public abstract class TagSupport {
  private static final Logger log = Logger.getLogger( TagSupport.class );
  private static final ConcurrentMap<String,TagSupport> supportByIdentifierPrefix = Maps.newConcurrentMap();
  private static final ConcurrentMap<Class<? extends CloudMetadata>,TagSupport> supportByClass = Maps.newConcurrentMap();
  private static final Splitter idSplitter = Splitter.on( '-' ).limit( 2 );

  private final Class<? extends AbstractPersistent> resourceClass;
  private final Class<? extends CloudMetadata> cloudMetadataClass;
  private final Set<String> identifierPrefixes;
  private final String resourceClassIdField;
  private final String tagClassResourceField;
  private final String notFoundErrorCode;
  private final String notFoundFormatString;

  protected <T extends AbstractPersistent & CloudMetadata> TagSupport( @Nonnull final Class<T> resourceClass,
                                                                       @Nonnull final Set<String> identifierPrefixes,
                                                                       @Nonnull final String resourceClassIdField,
                                                                       @Nonnull final String tagClassResourceField,
                                                                       @Nonnull final String notFoundErrorCode,
                                                                       @Nonnull final String notFoundFormatString ) {

    this.resourceClass = resourceClass;
    this.cloudMetadataClass = subclassFor( resourceClass );
    this.identifierPrefixes = ImmutableSet.copyOf( identifierPrefixes );
    this.resourceClassIdField = resourceClassIdField;
    this.tagClassResourceField = tagClassResourceField;
    this.notFoundErrorCode = notFoundErrorCode;
    this.notFoundFormatString = notFoundFormatString;
  }

  protected <T extends AbstractPersistent & CloudMetadata> TagSupport( @Nonnull final Class<T> resourceClass,
                                                                       @Nonnull final String identifierPrefix,
                                                                       @Nonnull final String resourceClassIdField,
                                                                       @Nonnull final String tagClassResourceField,
                                                                       @Nonnull final String notFoundErrorCode,
                                                                       @Nonnull final String notFoundFormatString ) {
    this(
        resourceClass,
        Collections.singleton( identifierPrefix ),
        resourceClassIdField,
        tagClassResourceField,
        notFoundErrorCode,
        notFoundFormatString );
  }

  public abstract Tag createOrUpdate( CloudMetadata metadata, 
                                      OwnerFullName ownerFullName,
                                      String key, 
                                      String value );

  public abstract Tag example( @Nonnull CloudMetadata metadata,
                               @Nonnull OwnerFullName ownerFullName,
                               @Nullable String key,
                               @Nullable String value );

  public abstract Tag example( @Nonnull OwnerFullName ownerFullName );

  protected  <T extends Tag> T example( @Nonnull T tag,
                                        @Nonnull OwnerFullName ownerFullName ) {
    tag.setOwner( ownerFullName );
    return tag;
  }

  public final long count( @Nonnull CloudMetadata metadata,
                           @Nonnull OwnerFullName ownerFullName ) {
    final Tag example = example( metadata, ownerFullName, null, null );
    return Tags.count( example );
  }

  public abstract CloudMetadata lookup( String identifier ) throws TransactionException;

  public final String getNotFoundErrorCode( ){
    return notFoundErrorCode;
  }

  public final String getNotFoundFormatString( ) {
    return notFoundFormatString;
  }

  /**
   * Get the tags for the given resources, grouped by ID and ordered for display.
   * 
   * @param owner The account for the tags
   * @param identifiers The resource identifiers for the tags
   * @return The tag map with an entry for each requested resource
   */
  public Map<String,List<Tag>> getResourceTagMap( final OwnerFullName owner,
                                                  final Iterable<String> identifiers ) {
    final int identifiersSize = Iterables.size( identifiers );
    final Map<String,List<Tag>> tagMap = Maps.newHashMapWithExpectedSize( identifiersSize );
    for ( final String id : identifiers ) {
      tagMap.put( id, Lists.<Tag>newArrayList() );
    }
    if ( !tagMap.isEmpty() ) {
      final Tag example = example( owner );
      final Criterion idRestriction = identifiersSize < 1000 ?
          Property.forName( tagClassResourceField ).in( DetachedCriteria.forClass( resourceClass )
              .add( Restrictions.in( resourceClassIdField, Lists.newArrayList( identifiers ) ) )
              .setProjection( Projections.id( ) ) ) :
          Restrictions.conjunction( );
      try {
        final List<Tag> tags = Tags.list( example, Predicates.alwaysTrue(), idRestriction, Collections.<String,String>emptyMap()  );
        for ( final Tag tag : tags ) {
          final List<Tag> keyTags = tagMap.get( tag.getResourceId( ) );
          if ( keyTags != null ) {
            keyTags.add( tag );
          }
        }
      } catch ( Exception e ) {
        log.error( e, e );
      }
      Ordering<Tag> order = Ordering.natural().onResultOf( Tags.key() );
      for ( final String id : identifiers ) {
        Collections.sort( tagMap.get( id ), order );
      }
    }
    return tagMap;
  }

  @Nonnull
  public Class<? extends CloudMetadata> getCloudMetadataClass() {
    return cloudMetadataClass;
  }

  @Nonnull
  public Set<String> getIdentifierPrefixes() {
    return identifierPrefixes;
  }

  public static TagSupport forResourceClass( @Nonnull final Class<? extends CloudMetadata> metadataClass ) {
    return supportByClass.get( subclassFor( metadataClass ) );
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
    return metadataClassMap.getUnchecked( metadataInstance );  
  }

  Class<? extends AbstractPersistent> getResourceClass() {
    return resourceClass;
  }

  String getResourceClassIdField() {
    return resourceClassIdField;
  }

  String getTagClassResourceField() {
    return tagClassResourceField;
  }

  private static final LoadingCache<Class,Class> metadataClassMap = CacheBuilder.newBuilder().build(
    new CacheLoader<Class,Class>() {
      @Override
      public Class load( final Class instanceClass ) {
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
    });
}
