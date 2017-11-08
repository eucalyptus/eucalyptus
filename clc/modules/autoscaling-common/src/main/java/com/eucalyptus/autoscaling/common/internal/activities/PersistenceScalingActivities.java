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
package com.eucalyptus.autoscaling.common.internal.activities;

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
import com.eucalyptus.autoscaling.common.internal.metadata.AbstractOwnedPersistents;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataNotFoundException;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.CollectionUtils;
import com.eucalyptus.util.Exceptions;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

/**
 *
 */
@ComponentNamed
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
    try {
      persistenceSupport.withRetries( ).updateByExample(
          persistenceSupport.exampleWithName( ownerFullName, activityId ),
          ownerFullName,
          activityId,
          activityUpdateCallback
      );
    } catch ( AutoScalingMetadataException e ) {
      Exceptions.findAndRethrow( e, AutoScalingMetadataNotFoundException.class );
      throw e;
    }
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
    protected ScalingActivity exampleWithOwner( final OwnerFullName ownerFullName ) {
      return ScalingActivity.withOwner( ownerFullName );
    }

    @Override
    protected ScalingActivity exampleWithName( final OwnerFullName ownerFullName, final String name ) {
      return ScalingActivity.named( ownerFullName, name );
    }
  }
}
