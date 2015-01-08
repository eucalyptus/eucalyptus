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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTaskMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.ActivityTask;
import com.eucalyptus.simpleworkflow.ActivityTasks;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceActivityTasks extends SwfPersistenceSupport<ActivityTaskMetadata,ActivityTask> implements ActivityTasks {

  public PersistenceActivityTasks( ) {
    super( "activity-task" );
  }

  public <T> List<T> listTimedOut( final Function<? super ActivityTask,T> transform ) throws SwfMetadataException {
    return listByExample(
        ActivityTask.exampleWithOwner( null ),
        Predicates.alwaysTrue(),
        Restrictions.lt( "timeoutTimestamp", new Date( ) ),
        Collections.<String,String>emptyMap( ),
        transform );
  }

  @Override
  public long countByWorkflowExecution( final OwnerFullName ownerFullName,
                                        final String domain,
                                        final String runId ) throws SwfMetadataException {
    return countByExample( ActivityTask.exampleWithWorkflowExecution( ownerFullName, domain, runId ) );
  }

  @Override
  protected ActivityTask exampleWithOwner( final OwnerFullName ownerFullName ) {
    return ActivityTask.exampleWithOwner( ownerFullName );
  }

  @Override
  protected ActivityTask exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return ActivityTask.exampleWithActivityId( ownerFullName, null, null, name );
  }
}
