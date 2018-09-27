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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.compute.common.CloudMetadata;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.ResourceTagSetItemType;
import com.eucalyptus.compute.common.ResourceTagSetType;
import com.eucalyptus.compute.common.ResourceTagged;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.compute.common.internal.util.NoSuchMetadataException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 * Utility functions for Tag
 */
public class Tags {

  /**
   * List tags for the given owner.
   * 
   * @param ownerFullName The tag owner
   * @return The list of tags.
   * @throws NoSuchMetadataException If an error occurs
   */
  public static List<Tag> list( final OwnerFullName ownerFullName ) throws NoSuchMetadataException {
    try {
      return Transactions.findAll( Tag.withOwner(ownerFullName) );
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find tags for " + ownerFullName, e );
    }
  }

  /**
   * List tags for the given owner.
   *
   * @param ownerFullName The tag owner
   * @param filter Predicate to restrict the results
   * @param criterion The database criterion to restrict the results
   * @param aliases Aliases for the database criterion
   * @return The list of tags.
   * @throws NoSuchMetadataException If an error occurs
   */
  public static List<Tag> list( final OwnerFullName ownerFullName,
                                final Predicate<? super Tag> filter,
                                final Criterion criterion,
                                final Map<String,String> aliases ) throws NoSuchMetadataException {
    return list( Tag.withOwner( ownerFullName ), filter, criterion, aliases );
  }

  /**
   * List tags matching the given example and criteria.
   *
   * @param example The tag example
   * @param filter Predicate to restrict the results
   * @param criterion The database criterion to restrict the results
   * @param aliases Aliases for the database criterion
   * @return The list of tags.
   * @throws NoSuchMetadataException If an error occurs
   */
  public static List<Tag> list( final Tag example,
                                final Predicate<? super Tag> filter,
                                final Criterion criterion,
                                final Map<String,String> aliases ) throws NoSuchMetadataException {
    try {
      return Transactions.filter( example, filter, criterion, aliases );
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find tags for " + LogUtil.dumpObject(example), e );
    }
  }

  /**
   * Count tags matching the given example and criteria.
   *
   * <p>The count will not include reserved tags.</p>
   *
   * @param example The tag example
   * @return The matching tag count.
   */
  public static long count( final Tag example ) {
    final EntityTransaction transaction = Entities.get( example );
    try {
      final Junction reservedTagKey = Restrictions.disjunction();
      reservedTagKey.add( Restrictions.like( "displayName", "aws:%" ) );
      reservedTagKey.add( Restrictions.like( "displayName", "euca:%" ) );
      return Entities.count(
          example,
          Restrictions.not( reservedTagKey ),
          Collections.<String,String>emptyMap() );
    } finally {
      transaction.rollback();
    }
  }

  public static Function<Tag,String> resourceId() {
    return TagFunctions.RESOURCE_ID;
  }

  public static Function<Tag,String> key() {
    return TagFunctions.KEY;
  }

  public static Function<Tag,String> value() {
    return TagFunctions.VALUE;
  }

  public static void delete( final Tag example ) throws NoSuchMetadataException {
    final EntityTransaction db = Entities.get(Tag.class);
    try {
      final Tag entity = Entities.uniqueResult( example );
      Entities.delete( entity );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust().error( ex, ex );
      db.rollback( );
      throw new NoSuchMetadataException( "Failed to find tag: " + example.getKey() + " for " + example.getOwner(), ex );
    }
  }

  /**
   * Create or update a tag.
   * 
   * <P>Caller must have active transaction for tags.</P>
   * 
   * @param tag The tag to create or update
   * @return The tag instance
   */
  public static Tag createOrUpdate( final Tag tag ) {
    Tag result;
    String originalValue = tag.getValue();
    String originalUserId = tag.getOwnerUserId();
    String originalUserName = tag.getOwnerUserName();
    try {
      tag.setValue( null );
      tag.setOwnerUserId( null );
      tag.setOwnerUserName( null );
      tag.setValue( null );
      final Tag existing = lookup( tag );
      existing.setValue( originalValue );
      result = existing;
    } catch ( final NoSuchMetadataException e ) {
      tag.setValue( originalValue );
      tag.setOwnerUserId( originalUserId );
      tag.setOwnerUserName( originalUserName );
      Entities.persist( tag );
      result = tag;
    } finally {
      tag.setValue( originalValue );
      tag.setOwnerUserId( originalUserId );
      tag.setOwnerUserName( originalUserName );
    }
    return result;
  }

  /**
   * Add transformed tags to a collection.
   * 
   * @param target The target collection
   * @param targetItemType The target item class
   * @param tags The tags to add
   * @param <T> The target item type
   */
  public static <T> void addFromTags( final Collection<? super T> target,
                                      final Class<T> targetItemType,
                                      final Iterable<Tag> tags ) {
    Iterables.addAll( target,
        Iterables.transform(
            tags,
            TypeMappers.lookup( Tag.class, targetItemType ) ) );
  }

  /**
   * Populate tags on a resource.
   *
   * @param accountFullName The tag account
   * @param resourceType The resource class
   * @param items The list of items for tag population
   * @param idFunction Extract resource identifier from an item
   * @param <RT> The tagged resource type
   */
  public static <RT extends ResourceTagged> void populateTags( final AccountFullName accountFullName,
                                                                final Class<? extends CloudMetadata> resourceType,
                                                                final List<? extends RT> items,
                                                                final Function<? super RT, String> idFunction ) {
    final Map<String,List<Tag>> tagsMap = TagSupport.forResourceClass( resourceType ).getResourceTagMap(
        accountFullName,
        Iterables.filter( Iterables.transform( items, idFunction ), Predicates.notNull( ) ) );
    for ( final RT item : items ) {
      final ResourceTagSetType tags = new ResourceTagSetType( );
      Tags.addFromTags(
          tags.getItem(),
          ResourceTagSetItemType.class,
          tagsMap.getOrDefault( idFunction.apply( item ), Collections.emptyList() ) );
      if ( !tags.getItem().isEmpty() ) {
        item.setTagSet( tags );
      }
    }
  }

  private static Tag lookup( final Tag example ) throws NoSuchMetadataException {
    try {
      final List<Tag> result = Transactions.filter( example,
          Predicates.compose( Predicates.equalTo( example.getResourceId() ), Tags.resourceId() ) );
      if ( result.size() == 1 ) {
        return result.get( 0 );
      }
    } catch ( Exception e ) {
      throw new NoSuchMetadataException( "Failed to find tag: " + example.getKey() + " for " + example.getOwner(), e );
    }

    throw new NoSuchMetadataException( "Failed to find unique tag: " + example.getKey() + " for " + example.getOwner() );
  }

  public static class TagFilterSupport extends FilterSupport<Tag> {
    public TagFilterSupport() {
      super( builderFor( Tag.class )
          .withStringProperty( "key", TagFunctions.KEY )
          .withStringProperty( "resource-id", TagFunctions.RESOURCE_ID )
          .withStringProperty( "resource-type", TagFunctions.RESOURCE_TYPE )
          .withStringProperty( "value", TagFunctions.VALUE )
          .withPersistenceFilter( "key", "displayName" )
          .withPersistenceFilter( "value" )
      );
    }
  }

  private enum TagFunctions implements Function<Tag,String> {
    KEY {
      @Override
      public String apply(final Tag tag ) {
        return tag.getKey();
      }
    },
    RESOURCE_TYPE {
      @Override
      public String apply(final Tag tag ) {
        return tag.getResourceType();
      }
    },
    RESOURCE_ID {
      @Override
      public String apply(final Tag tag ) {
        return tag.getResourceId();
      }
    },
    VALUE {
      @Override
      public String apply(final Tag tag ) {
        return tag.getValue();
      }
    },
  }

  @TypeMapper
  public enum TagToResourceTag implements Function<Tag, ResourceTag> {
    INSTANCE;

    @Override
    public ResourceTag apply( final Tag tag ) {
      return new ResourceTag( tag.getKey(), tag.getValue() );
    }
  }

  @TypeMapper
  public enum TagToTagInfo implements Function<Tag, TagInfo> {
    INSTANCE;

    @Override
    public TagInfo apply( final Tag tag ) {
      final TagInfo info = new TagInfo();
      info.setKey( tag.getKey() );
      info.setValue( tag.getValue() );
      info.setResourceId( tag.getResourceId() );
      info.setResourceType( tag.getResourceType() );
      return info;
    }
  }
}
