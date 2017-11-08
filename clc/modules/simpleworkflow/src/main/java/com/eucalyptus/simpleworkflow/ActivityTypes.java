/*************************************************************************
 * Copyright 2009-2014 Ent. Services Development Corporation LP
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
                  .withDefaultTaskStartToCloseTimeout( timeout( activityType.getDefaultTaskStartToCloseTimeout() ) )
                  .withDefaultTaskPriority(priority(activityType.getDefaultTaskPriority())))
              .withTypeInfo( TypeMappers.transform( activityType, ActivityTypeInfo.class ) );
    }

    private String timeout( Integer timeout ) {
      timeout = timeout == -1 ? null : timeout;
      return Optional.fromNullable( timeout )
          .transform( Functions.toStringFunction() )
          .or( "NONE" );
    }
    
    private String priority( final Integer priority ) {
      return Optional.fromNullable( priority )
          .transform( Functions.toStringFunction() )
          .or("0");
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
