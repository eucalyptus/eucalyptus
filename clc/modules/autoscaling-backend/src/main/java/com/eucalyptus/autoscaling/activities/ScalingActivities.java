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
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.autoscaling.common.backend.msgs.Activity;
import com.eucalyptus.autoscaling.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.Strings;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public abstract class ScalingActivities {

  public abstract <T> List<T> list( @Nullable OwnerFullName ownerFullName,
                                    @Nonnull  Predicate<? super ScalingActivity> filter,
                                    @Nonnull  Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException;

  /**
   * List scaling activities with optional filters by group and id.
   *
   * @param ownerFullName The activity owner
   * @param group The activity group
   * @param activityIds The activity ids of interest (empty for any)
   * @param filter Additional ScalingActivity filter predicate
   * @return The list of matching ScalingActivities
   * @throws AutoScalingMetadataException If an error occurs.
   */
  public abstract <T> List<T> list( @Nullable OwnerFullName ownerFullName,
                                    @Nullable AutoScalingGroupMetadata group,
                                    @Nonnull  Collection<String> activityIds,
                                    @Nonnull  Predicate<? super ScalingActivity> filter,
                                    @Nonnull  Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException;

  public abstract <T> List<T> listByActivityStatusCode( @Nullable OwnerFullName ownerFullName,
                                                        @Nonnull  Collection<ActivityStatusCode> statusCodes,
                                                        @Nonnull  Function<? super ScalingActivity,T> transform ) throws AutoScalingMetadataException;

  public abstract void update( OwnerFullName ownerFullName,
                               String activityId,
                               Callback<ScalingActivity> activityUpdateCallback ) throws AutoScalingMetadataException;

  public abstract boolean delete( ScalingActivityMetadata scalingActivity ) throws AutoScalingMetadataException;

  public abstract int deleteByCreatedAge( @Nullable OwnerFullName ownerFullName,
                                          long createdBefore ) throws AutoScalingMetadataException;

  public abstract ScalingActivity save( ScalingActivity scalingActivity ) throws AutoScalingMetadataException;

  public static Function<ScalingActivity,AutoScalingGroup> group() {
    return ScalingActivityToGroup.INSTANCE;
  }

  @TypeMapper
  public enum ScalingActivityTransform implements Function<ScalingActivity, Activity> {
    INSTANCE;

    @Override
    public Activity apply( final ScalingActivity activity ) {
      final Activity type = new Activity();

      type.setActivityId( activity.getActivityId() );      
      type.setAutoScalingGroupName( activity.getAutoScalingGroupName() );
      type.setCause( activity.getCauseAsString( ) );
      type.setDescription( activity.getDescription() );
      type.setDetails( activity.getDetails() );
      type.setEndTime( activity.getEndTime() );
      type.setProgress( activity.getProgress() );
      type.setStartTime( activity.getCreationTimestamp() );
      type.setStatusCode( Strings.toString( activity.getStatusCode( ) ) );
      type.setStatusMessage( activity.getStatusMessage() );

      return type;
    }
  }

  private enum ScalingActivityToGroup implements Function<ScalingActivity,AutoScalingGroup> {
    INSTANCE;

    @Override
    public AutoScalingGroup apply( @Nullable final ScalingActivity scalingActivity ) {
      return scalingActivity == null ? null : scalingActivity.getGroup();
    }
  }
}
