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
package com.eucalyptus.autoscaling.tags;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityTransaction;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.autoscaling.common.backend.msgs.TagDescription;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.Transactions;
import com.eucalyptus.records.Logs;
import com.eucalyptus.util.LogUtil;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

/**
 *
 */
public class Tags {

  /**
   * List tags for the given owner.
   *
   * @param ownerFullName The tag owner
   * @return The list of tags.
   * @throws AutoScalingMetadataNotFoundException If an error occurs
   */
  public static List<Tag> list( final OwnerFullName ownerFullName ) throws AutoScalingMetadataNotFoundException {
    try {
      return Transactions.findAll( Tag.withOwner( ownerFullName ) );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataNotFoundException( "Failed to find tags for " + ownerFullName, e );
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
   * @throws AutoScalingMetadataNotFoundException If an error occurs
   */
  public static List<Tag> list( final OwnerFullName ownerFullName,
                                                    final Predicate<? super Tag> filter,
                                                    final Criterion criterion,
                                                    final Map<String,String> aliases ) throws AutoScalingMetadataNotFoundException {
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
   * @throws AutoScalingMetadataNotFoundException If an error occurs
   */
  public static List<Tag> list( final Tag example,
                                final Predicate<? super Tag> filter,
                                final Criterion criterion,
                                final Map<String,String> aliases ) throws AutoScalingMetadataNotFoundException {
    try {
      return Transactions.filter( example, filter, criterion, aliases );
    } catch ( Exception e ) {
      throw new AutoScalingMetadataNotFoundException( "Failed to find tags for " + LogUtil.dumpObject( example ), e );
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

  public static Function<Tag,String> propagateAtLaunch() {
    return TagFunctions.PROPAGATE_AT_LAUNCH;
  }

  public static void delete( final Tag example ) throws AutoScalingMetadataNotFoundException {
    final EntityTransaction db = Entities.get( Tag.class );
    try {
      final Tag entity = Entities.uniqueResult( example );
      Entities.delete( entity );
      db.commit( );
    } catch ( Exception ex ) {
      Logs.exhaust().error( ex, ex );
      db.rollback( );
      throw new AutoScalingMetadataNotFoundException( "Failed to find tag: " + example.getKey() + " for " + example.getOwner(), ex );
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
    final String originalValue = tag.getValue();
    final Boolean originalPropagate = tag.getPropagateAtLaunch();
    final String originalUserId = tag.getOwnerUserId();
    final String originalUserName = tag.getOwnerUserName();
    try {
      tag.setValue( null );
      tag.setOwnerUserId( null );
      tag.setOwnerUserName( null );
      tag.setPropagateAtLaunch( null );
      final Tag existing = lookup( tag );
      existing.setValue( originalValue );
      existing.setPropagateAtLaunch( originalPropagate );
      result = existing;
    } catch ( final AutoScalingMetadataNotFoundException e ) {
      tag.setValue( originalValue );
      tag.setPropagateAtLaunch( originalPropagate );
      tag.setOwnerUserId( originalUserId );
      tag.setOwnerUserName( originalUserName );
      Entities.persist( tag );
      result = tag;
    } finally {
      tag.setValue( originalValue );
      tag.setOwnerUserId( originalUserId );
      tag.setOwnerUserName( originalUserName );
      tag.setPropagateAtLaunch( originalPropagate );
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

  private static Tag lookup( final Tag example ) throws AutoScalingMetadataNotFoundException {
    try {
      final List<Tag> result = Transactions.filter( example,
          Predicates.compose( Predicates.equalTo( example.getResourceId() ), Tags.resourceId() ) );
      if ( result.size() == 1 ) {
        return result.get( 0 );
      }
    } catch ( Exception e ) {
      throw new AutoScalingMetadataNotFoundException( "Failed to find tag: " + example.getKey() + " for " + example.getOwner(), e );
    }

    throw new AutoScalingMetadataNotFoundException( "Failed to find unique tag: " + example.getKey() + " for " + example.getOwner() );
  }

  private enum TagFunctions implements Function<Tag,String> {
    KEY {
      @Override
      public String apply(final Tag tag ) {
        return tag.getKey();
      }
    },
    PROPAGATE_AT_LAUNCH {
      @Override
      public String apply(final Tag tag ) {
        return Strings.toString( tag.getPropagateAtLaunch() );
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
  public enum TagToTagDescription implements Function<Tag, TagDescription> {
    INSTANCE;

    @Override
    public TagDescription apply( final Tag tag ) {
      final TagDescription tagDescription = new TagDescription();
      tagDescription.setResourceType( tag.getResourceType() );
      tagDescription.setResourceId( tag.getResourceId() );
      tagDescription.setKey( tag.getKey() );
      tagDescription.setValue( tag.getValue() );
      tagDescription.setPropagateAtLaunch( tag.getPropagateAtLaunch() );
      return tagDescription;
    }
  }
}
