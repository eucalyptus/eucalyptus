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
package com.eucalyptus.simpleworkflow.persist;

import static com.eucalyptus.simpleworkflow.SimpleWorkflowConfiguration.getWorkflowExecutionDurationMillis;
import static com.eucalyptus.simpleworkflow.SimpleWorkflowConfiguration.getWorkflowExecutionRetentionDurationMillis;
import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.WorkflowExecutionMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.SimpleWorkflowConfiguration;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.WorkflowExecution;
import com.eucalyptus.simpleworkflow.WorkflowExecutions;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceWorkflowExecutions extends SwfPersistenceSupport<WorkflowExecutionMetadata,WorkflowExecution> implements WorkflowExecutions {

  public PersistenceWorkflowExecutions( ) {
    super( "workflow-execution" );
  }

  public <T> List<T> listTimedOut( final long time,
                                   final Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException {
    return listByExample(
        WorkflowExecution.exampleForOpenWorkflow(),
        Predicates.alwaysTrue( ),
        Restrictions.disjunction( )
            .add( Restrictions.lt( "timeoutTimestamp", new Date( time ) ) )
            .add( Restrictions.lt( "creationTimestamp", new Date( time - getWorkflowExecutionDurationMillis() ) ) ),
        Collections.<String,String>emptyMap( ),
        transform );
  }

  public <T> List<T> listRetentionExpired( final long time,
                                           final Function<? super WorkflowExecution,T> transform ) throws SwfMetadataException {
    return listByExample(
        WorkflowExecution.exampleForClosedWorkflow(),
        Predicates.alwaysTrue(),
        Restrictions.disjunction()
            .add( Restrictions.lt( "retentionTimestamp", new Date( time ) ) )
            .add( Restrictions.lt( "closeTimestamp", new Date( time - getWorkflowExecutionRetentionDurationMillis() ) ) ),
        Collections.<String, String>emptyMap(),
        transform );
  }

  @Override
  public long countOpenByDomain( final OwnerFullName ownerFullName,
                                 final String domain ) throws SwfMetadataException {
    return countByExample( WorkflowExecution.exampleForOpenWorkflow( ownerFullName, domain, null ) );
  }

  @Override
  protected WorkflowExecution exampleWithOwner( final OwnerFullName ownerFullName ) {
    return WorkflowExecution.exampleWithOwner( ownerFullName );
  }

  @Override
  protected WorkflowExecution exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return WorkflowExecution.exampleWithName( ownerFullName, name );
  }
}
