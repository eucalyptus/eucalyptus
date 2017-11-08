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
package com.eucalyptus.autoscaling.tags;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;

import com.eucalyptus.autoscaling.common.AutoScalingMetadata;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.entities.AbstractOwnedPersistent;
import com.eucalyptus.entities.TransactionException;
import com.eucalyptus.util.Classes;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

/**
 * Support functionality for each resource that supports tagging
 */
public abstract class TagSupport {
  private static final Logger log = Logger.getLogger( TagSupport.class );
  private static final ConcurrentMap<String,TagSupport> supportByResourceType = Maps.newConcurrentMap();
  private static final ConcurrentMap<Class<? extends AutoScalingMetadata>,TagSupport> supportByClass = Maps.newConcurrentMap();
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
                Predicates.not( Predicates.<Class<?>>equalTo( AutoScalingMetadata.class ) ),
                Classes.subclassOf( AutoScalingMetadata.class ) ) );
      }
    });

  private final Class<? extends AbstractOwnedPersistent> resourceClass;
  private final Class<? extends AutoScalingMetadata> cloudMetadataClass;
  private final String resourceType;
  private final String resourceClassIdField;
  private final String tagClassResourceField;

  protected <T extends AbstractOwnedPersistent & AutoScalingMetadata>
    TagSupport( @Nonnull final Class<T> resourceClass,
                @Nonnull final String resourceType,
                @Nonnull final String resourceClassIdField,
                @Nonnull final  String tagClassResourceField ) {

    this.resourceClass = resourceClass;
    this.cloudMetadataClass = subclassFor( resourceClass );
    this.resourceType = resourceType;
    this.resourceClassIdField = resourceClassIdField;
    this.tagClassResourceField = tagClassResourceField;
  }

  public abstract Tag createOrUpdate( AutoScalingMetadata metadata,
                                      OwnerFullName ownerFullName,
                                      String key,
                                      String value,
                                      Boolean propagateAtLaunch );

  public abstract Tag example( @Nonnull AutoScalingMetadata metadata,
                               @Nonnull OwnerFullName ownerFullName,
                               @Nullable String key,
                               @Nullable String value );

  public abstract Tag example( @Nonnull OwnerFullName ownerFullName );

  protected  <T extends Tag> T example( @Nonnull T tag,
                                        @Nonnull OwnerFullName ownerFullName ) {
    tag.setOwner( ownerFullName );
    return tag;
  }

  public abstract AutoScalingMetadata lookup( OwnerFullName owner, String identifier ) throws TransactionException;

  /**
   * Get the tags for the given resources, grouped by ID and ordered for display.
   *
   * @param owner The account for the tags
   * @param identifiers The resource identifiers for the tags
   * @param tagPredicate Predicate for filtering tags
   * @return The tag map with an entry for each requested resource
   */
  public Map<String,List<Tag>> getResourceTagMap( final OwnerFullName owner,
                                                  final Iterable<String> identifiers,
                                                  final Predicate<? super Tag> tagPredicate) {
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
        final List<Tag> tags = Tags.list(
            example,
            tagPredicate,
            idRestriction,
            Collections.<String, String>emptyMap() );
        for ( final Tag tag : tags ) {
          tagMap.get( tag.getResourceId() ).add( tag );
        }
      } catch ( AutoScalingMetadataNotFoundException e ) {
        log.error( e, e );
      }
      Ordering<Tag> order = Ordering.natural().onResultOf( Tags.key() );
      for ( final String id : identifiers ) {
        Collections.sort( tagMap.get( id ), order );
      }
    }
    return tagMap;
  }

  /**
   * Get the tags for the given resources, grouped by ID and ordered for display.
   *
   * @param owner The account for the tags
   * @param identifier The resource identifier for the tags
   * @param tagPredicate Predicate for filtering tags
   * @return The tags for the resource
   */
  public List<Tag> getResourceTags( final OwnerFullName owner,
                                    final String identifier,
                                    final Predicate<? super Tag> tagPredicate ) {
    return getResourceTagMap( owner, Collections.singleton( identifier ), tagPredicate ).get( identifier );
  }

  @Nonnull
  public Class<? extends AutoScalingMetadata> getCloudMetadataClass() {
    return cloudMetadataClass;
  }

  @Nonnull
  public String getResourceType() {
    return resourceType;
  }

  public static TagSupport forResourceClass( @Nonnull final Class<? extends AutoScalingMetadata> metadataClass ) {
    return supportByClass.get( subclassFor( metadataClass ) );
  }

  public static TagSupport fromResource( @Nonnull final AutoScalingMetadata metadata ) {
    return supportByClass.get( subclassFor( metadata.getClass() ) );
  }

  public static TagSupport fromResourceType( @Nonnull final String resourceType ) {
    return supportByResourceType.get( resourceType );
  }

  static void registerTagSupport( @Nonnull final TagSupport tagSupport ) {
    supportByClass.put( tagSupport.getCloudMetadataClass(), tagSupport );
    supportByResourceType.put( tagSupport.getResourceType(), tagSupport );
  }

  @SuppressWarnings( "unchecked" )
  private static Class<? extends AutoScalingMetadata> subclassFor( Class<? extends AutoScalingMetadata> metadataInstance ) {
    return metadataClassMap.getUnchecked( metadataInstance );
  }

  Class<? extends AbstractOwnedPersistent> getResourceClass() {
    return resourceClass;
  }

  String getResourceClassIdField() {
    return resourceClassIdField;
  }

  String getTagClassResourceField() {
    return tagClassResourceField;
  }

}
