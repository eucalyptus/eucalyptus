/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
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
package com.eucalyptus.simpleworkflow;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeConfiguration;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.ActivityTypeInfo;
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.util.Callback;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 *
 */
public interface ActivityTypes {

  <T> T lookupByExample( ActivityType example,
                         @Nullable OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super ActivityType> filter,
                         Function<? super ActivityType,T> transform ) throws SwfMetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Predicate<? super ActivityType> filter,
                    Function<? super ActivityType,T> transform ) throws SwfMetadataException;

  <T> List<T> listDeprecatedExpired( long time,
                                     Function<? super ActivityType,T> transform ) throws SwfMetadataException;

  ActivityType updateByExample( ActivityType example,
                                OwnerFullName ownerFullName,
                                String key,
                                Callback<ActivityType> updateCallback ) throws SwfMetadataException;

  ActivityType save( ActivityType activityType ) throws SwfMetadataException;

  long countByDomain( OwnerFullName ownerFullName, String domain ) throws SwfMetadataException;

  List<ActivityType> deleteByExample( ActivityType example ) throws SwfMetadataException;

  @TypeMapper
  public enum ActivityTypeToActivityTypeDetailTransform implements Function<ActivityType,ActivityTypeDetail> {
    INSTANCE;

    @Nullable
    @Override
    public ActivityTypeDetail apply( @Nullable final ActivityType activityType ) {
      return activityType == null ?
          null :
          new ActivityTypeDetail( )
              .withConfiguration( new ActivityTypeConfiguration( )
                  .withDefaultTaskList( new TaskList().withName( activityType.getDefaultTaskList() ) )
                  .withDefaultTaskHeartbeatTimeout( timeout( activityType.getDefaultTaskHeartbeatTimeout( ) ) )
                  .withDefaultTaskScheduleToCloseTimeout( timeout( activityType.getDefaultTaskScheduleToCloseTimeout( ) ) )
                  .withDefaultTaskScheduleToStartTimeout( timeout( activityType.getDefaultTaskScheduleToStartTimeout( ) ) )
                  .withDefaultTaskStartToCloseTimeout( timeout( activityType.getDefaultTaskStartToCloseTimeout() ) ) )
              .withTypeInfo( TypeMappers.transform( activityType, ActivityTypeInfo.class ) );
    }

    private String timeout( final Integer timeout ) {
      return Optional.fromNullable( timeout )
          .transform( Functions.toStringFunction() )
          .or( "NONE" );
    }
  }

  @TypeMapper
  public enum ActivityTypeToActivityTypeInfoTransform implements Function<ActivityType,ActivityTypeInfo> {
    INSTANCE;

    @Nullable
    @Override
    public ActivityTypeInfo apply( @Nullable final ActivityType activityType ) {
      return activityType == null ?
          null :
          new ActivityTypeInfo( )
              .withActivityType( new com.eucalyptus.simpleworkflow.common.model.ActivityType( )
                  .withName( activityType.getDisplayName( ) )
                  .withVersion( activityType.getActivityVersion( ) ) )
              .withDescription( activityType.getDescription() )
              .withCreationDate( activityType.getCreationTimestamp( ) )
              .withDeprecationDate( activityType.getDeprecationTimestamp( ) )
              .withStatus( Objects.toString( activityType.getState( ), null ) );
    }
  }

  enum StringFunctions implements Function<ActivityType,String> {
    DOMAIN {
      @Nullable
      @Override
      public String apply( @Nullable final ActivityType activityType ) {
        return activityType == null ?
            null :
            SimpleWorkflowMetadatas.toDisplayName().apply( activityType.getDomain() );
      }
    },
    REGISTRATION_STATUS {
      @Nullable
      @Override
      public String apply( @Nullable final ActivityType activityType ) {
        return activityType == null ?
            null :
            Objects.toString( activityType.getState( ), null );
      }
    },
  }

  enum ActivityTypeInfoStringFunctions implements Function<ActivityTypeInfo,String> {
    NAME {
      @Nullable
      @Override
      public String apply( @Nullable final ActivityTypeInfo activityTypeInfo ) {
        return activityTypeInfo == null || activityTypeInfo.getActivityType( ) == null ?
            null :
            activityTypeInfo.getActivityType( ).getName( );
      }
    },
  }
}
