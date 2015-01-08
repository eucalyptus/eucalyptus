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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.cloud.util.NoSuchMetadataException;
import com.eucalyptus.compute.common.ResourceTag;
import com.eucalyptus.compute.common.TagInfo;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
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
