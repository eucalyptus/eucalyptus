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
import com.eucalyptus.entities.AbstractPersistentSupport;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public interface ActivityTasks {

  <T> List<T> listByExample( ActivityTask example,
                             Predicate<? super ActivityTask> filter,
                             Function<? super ActivityTask,T> transform ) throws SwfMetadataException;

  <T> List<T> listTimedOut( Function<? super ActivityTask,T> transform ) throws SwfMetadataException;

  <T> T updateByExample( ActivityTask example,
                         OwnerFullName ownerFullName,
                         String activityId,
                         Function<? super ActivityTask,T> updateTransform ) throws SwfMetadataException;


  ActivityTask save( ActivityTask activityTask ) throws SwfMetadataException;

  List<ActivityTask> deleteByExample( ActivityTask example ) throws SwfMetadataException;

  long countByWorkflowExecution( OwnerFullName ownerFullName,
                                 String domain,
                                 String runId ) throws SwfMetadataException;

  AbstractPersistentSupport<SimpleWorkflowMetadata.ActivityTaskMetadata,ActivityTask,SwfMetadataException> withRetries( );


}
