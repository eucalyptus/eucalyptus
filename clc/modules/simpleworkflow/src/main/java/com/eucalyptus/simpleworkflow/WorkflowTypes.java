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
import com.eucalyptus.simpleworkflow.common.model.TaskList;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeConfiguration;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeDetail;
import com.eucalyptus.simpleworkflow.common.model.WorkflowTypeInfo;
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
public interface WorkflowTypes {

  <T> T lookupByExample( WorkflowType example,
                         @Nullable OwnerFullName ownerFullName,
                         String key,
                         Predicate<? super WorkflowType> filter,
                         Function<? super WorkflowType,T> transform ) throws SwfMetadataException;

  <T> List<T> list( OwnerFullName ownerFullName,
                    Predicate<? super WorkflowType> filter,
                    Function<? super WorkflowType,T> transform ) throws SwfMetadataException;

  <T> List<T> listDeprecatedExpired( long time,
                                     Function<? super WorkflowType,T> transform ) throws SwfMetadataException;

  WorkflowType updateByExample( WorkflowType example,
                                OwnerFullName ownerFullName,
                                String key,
                                Callback<WorkflowType> updateCallback ) throws SwfMetadataException;

  WorkflowType save( WorkflowType workflowType ) throws SwfMetadataException;

  long countByDomain( OwnerFullName ownerFullName, String domain ) throws SwfMetadataException;

  List<WorkflowType> deleteByExample( WorkflowType example ) throws SwfMetadataException;

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

  enum WorkflowTypeInfoStringFunctions implements Function<WorkflowTypeInfo,String> {
    NAME {
      @Nullable
      @Override
      public String apply( @Nullable final WorkflowTypeInfo workflowTypeInfo ) {
        return workflowTypeInfo == null || workflowTypeInfo.getWorkflowType( ) == null ?
            null :
            workflowTypeInfo.getWorkflowType( ).getName( );
      }
    },
  }
}
