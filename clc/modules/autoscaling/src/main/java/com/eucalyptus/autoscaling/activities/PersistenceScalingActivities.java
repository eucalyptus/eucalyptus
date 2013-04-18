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
package com.eucalyptus.autoscaling.activities;

import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.AutoScalingGroupMetadata;
import static com.eucalyptus.autoscaling.common.AutoScalingMetadata.ScalingActivityMetadata;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.autoscaling.common.AutoScalingMetadatas;
import com.eucalyptus.autoscaling.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 *
 */
public class PersistenceScalingActivities extends ScalingActivities {

  private PersistenceSupport persistenceSupport = new PersistenceSupport();

  @Override
  public <T> List<T> list( @Nullable final OwnerFullName ownerFullName,
                           @Nonnull  final Predicate<? super ScalingActivity> filter,
                           @Nonnull  final Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException {
    return persistenceSupport.list( ownerFullName, filter, transform );
  }

  @Override
  public <T> List<T> list( @Nullable final OwnerFullName ownerFullName,
                           @Nullable final AutoScalingGroupMetadata group,
                           @Nonnull  final Collection<String> activityIds,
                           @Nonnull  final Predicate<? super ScalingActivity> filter,
                           @Nonnull  final Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException {
    final ScalingActivity example = ScalingActivity.withOwner( ownerFullName );
    final Conjunction conjunction = Restrictions.conjunction();
    final Collection<Predicate<? super ScalingActivity>> predicates = Lists.newArrayList();
    predicates.add( filter );
    if ( group != null ) {
      predicates.add( CollectionUtils.propertyPredicate( group.getArn(), Functions.compose( AutoScalingMetadatas.toArn(), ScalingActivities.group() ) ) );
      conjunction.add( Restrictions.eq( "autoScalingGroupName", group.getDisplayName() ) );
    }
    if ( !activityIds.isEmpty() ) {
      conjunction.add( Restrictions.in( "displayName", activityIds ) );
    }
    return persistenceSupport.listByExample(
        example,
        Predicates.and( predicates ),
        conjunction,
        Collections.<String,String>emptyMap(),
        transform );
  }

  @Override
  public <T> List<T> listByActivityStatusCode( @Nullable final OwnerFullName ownerFullName,
                                               @Nonnull  final Collection<ActivityStatusCode> statusCodes,
                                               @Nonnull  final Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException {
    final ScalingActivity example = ScalingActivity.withOwner( ownerFullName );
    final Conjunction conjunction = Restrictions.conjunction();
    if ( !statusCodes.isEmpty() ) {
      conjunction.add( Restrictions.in( "statusCode", statusCodes ) );
    }
    return persistenceSupport.listByExample(
        example,
        Predicates.alwaysTrue(),
        conjunction,
        Collections.<String, String>emptyMap(),
        transform );
  }

  @Override
  public void update( final OwnerFullName ownerFullName,
                      final String activityId,
                      final Callback<ScalingActivity> activityUpdateCallback ) throws AutoScalingMetadataException {
    persistenceSupport.updateByExample(
        persistenceSupport.exampleWithName( ownerFullName, activityId ),
        ownerFullName,
        activityId,
        activityUpdateCallback
    );
  }

  @Override
  public boolean delete( final ScalingActivityMetadata scalingActivity ) throws AutoScalingMetadataException {
    return persistenceSupport.delete( scalingActivity );
  }

  @Override
  public int deleteByCreatedAge( @Nullable final OwnerFullName ownerFullName,
                                 final long createdBefore ) throws AutoScalingMetadataException {
    return persistenceSupport.deleteByExample(
        ScalingActivity.withOwner( ownerFullName ),
        Restrictions.lt( "creationTimestamp", new Date( createdBefore ) ),
        Collections.<String,String>emptyMap() ).size();
  }

  @Override
  public ScalingActivity save( final ScalingActivity scalingActivity ) throws AutoScalingMetadataException {
    return persistenceSupport.save( scalingActivity );
  }

  private static class PersistenceSupport extends AbstractOwnedPersistents<ScalingActivity> {
    private PersistenceSupport() {
      super( "scaling activity" );
    }

    @Override
    protected ScalingActivity exampleWithUuid( final String uuid ) {
      return ScalingActivity.withUuid( uuid );
    }

    @Override
    protected ScalingActivity exampleWithOwner( final OwnerFullName ownerFullName ) {
      return ScalingActivity.withOwner( ownerFullName );
    }

    @Override
    protected ScalingActivity exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return ScalingActivity.named( ownerFullName, name );

    }
  }
}
