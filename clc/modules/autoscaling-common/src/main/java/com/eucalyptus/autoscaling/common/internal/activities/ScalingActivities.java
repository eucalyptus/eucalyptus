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
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.eucalyptus.autoscaling.common.msgs.Activity;
import com.eucalyptus.autoscaling.common.internal.groups.AutoScalingGroup;
import com.eucalyptus.autoscaling.common.internal.metadata.AutoScalingMetadataException;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
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
