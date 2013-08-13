/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
import com.eucalyptus.cloud.CloudMetadata;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.entities.AbstractPersistent;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.Classes;
import com.eucalyptus.util.OwnerFullName;
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
    final Map<String,List<Tag>> tagMap = Maps.newHashMap();
    for ( final String id : identifiers ) {
      tagMap.put( id, Lists.<Tag>newArrayList() );
    }
    if ( !tagMap.isEmpty() ) {
      final Tag example = example( owner );
      final DetachedCriteria detachedCriteria = DetachedCriteria.forClass( resourceClass )
          .add( Restrictions.in( resourceClassIdField, Lists.newArrayList( identifiers ) ) )
          .setProjection( Projections.id() );
      final Criterion idRestriction = Property.forName( tagClassResourceField ).in( detachedCriteria );
      try {
        final List<Tag> tags = Tags.list( example, Predicates.alwaysTrue(), idRestriction, Collections.<String,String>emptyMap()  );
        for ( final Tag tag : tags ) {
          tagMap.get( tag.getResourceId() ).add( tag );
        }
      } catch ( NoSuchMetadataException e ) {
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
