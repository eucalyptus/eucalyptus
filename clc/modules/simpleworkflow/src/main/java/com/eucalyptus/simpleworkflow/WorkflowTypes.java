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
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.hibernate.criterion.Criterion;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadatas;
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeConfiguration;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfo;
import com.eucalyptus.util.Callback;
import com.eucalyptus.util.OwnerFullName;
import com.eucalyptus.util.TypeMapper;
import com.eucalyptus.util.TypeMappers;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 *
 */
public interface WorkflowTypes {

  <T> T lookupByExample( WorkflowType example,
                         @Nullable OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super WorkflowType> filter,
                         Function<? super WorkflowType,T> transform ) throws SwfMetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Criterion criterion,
                    Map<String,String> aliases,
                    Predicate<? super WorkflowType> filter,
                    Function<? super WorkflowType,T> transform ) throws SwfMetadataException;

  WorkflowType updateByExample( WorkflowType example,
                                OwnerFullName ownerFullName,
                                String key,
                                Callback<WorkflowType> updateCallback ) throws SwfMetadataException;

  WorkflowType save( WorkflowType workflowType ) throws SwfMetadataException;

  @TypeMapper
  public enum WorkflowTypeToWorkflowTypeDetailTransform implements Function<WorkflowType,WorkflowTypeDetail> {
    INSTANCE;

    @Nullable
    @Override
    public WorkflowTypeDetail apply( @Nullable final WorkflowType workflowType ) {
      return workflowType == null ?
          null :
          new WorkflowTypeDetail( )
              .withConfiguration( new WorkflowTypeConfiguration( )
                  .withDefaultTaskList( new TaskList( ).withName( workflowType.getDefaultTaskList( ) ) )
                  .withDefaultChildPolicy( workflowType.getDefaultChildPolicy() )
                  .withDefaultExecutionStartToCloseTimeout( timeout( workflowType.getDefaultExecutionStartToCloseTimeout( ) ) )
                  .withDefaultTaskStartToCloseTimeout( timeout( workflowType.getDefaultTaskStartToCloseTimeout( )  ) ) )
              .withTypeInfo( TypeMappers.transform( workflowType, WorkflowTypeInfo.class ) );
    }

    private String timeout( final Integer timeout ) {
      return Optional.fromNullable( timeout )
          .transform( Functions.toStringFunction( ) )
          .or( "NONE" );
    }
  }

  @TypeMapper
  public enum WorkflowTypeToWorkflowTypeInfoTransform implements Function<WorkflowType,WorkflowTypeInfo> {
    INSTANCE;

    @Nullable
    @Override
    public WorkflowTypeInfo apply( @Nullable final WorkflowType workflowType ) {
      return workflowType == null ?
          null :
          new WorkflowTypeInfo( )
              .withWorkflowType( new com.eucalyptus.simpleworkflow.common.model.WorkflowType( )
                  .withName( workflowType.getDisplayName( ) )
                  .withVersion( workflowType.getWorkflowVersion( ) ) )
              .withDescription( workflowType.getDescription() )
              .withCreationDate( workflowType.getCreationTimestamp( ) )
              .withDeprecationDate( workflowType.getDeprecationTimestamp( ) )
              .withStatus( Objects.toString( workflowType.getState( ), null ) );
    }
  }

  enum StringFunctions implements Function<WorkflowType,String> {
    DOMAIN {
      @Nullable
      @Override
      public String apply( @Nullable final WorkflowType workflowType ) {
        return workflowType == null ?
            null :
            SimpleWorkflowMetadatas.toDisplayName( ).apply( workflowType.getDomain() );
      }
    },
    REGISTRATION_STATUS {
      @Nullable
      @Override
      public String apply( @Nullable final WorkflowType workflowType ) {
        return workflowType == null ?
            null :
            Objects.toString( workflowType.getState( ), null );
      }
    },
  }
}
