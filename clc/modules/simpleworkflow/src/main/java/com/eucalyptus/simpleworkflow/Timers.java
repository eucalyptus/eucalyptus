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
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 *
 */
public interface Timers {

  <T> List<T> listByExample( Timer example,
                             Predicate<? super Timer> filter,
                             Function<? super Timer,T> transform ) throws SwfMetadataException;

  <T> List<T> listFired( Function<? super Timer,T> transform ) throws SwfMetadataException;

  <T> T updateByExample( Timer example,
                         OwnerFullName ownerFullName,
                         String timerId,
                         Function<? super Timer,T> updateTransform ) throws SwfMetadataException;


  Timer save( Timer timer ) throws SwfMetadataException;

  List<Timer> deleteByExample( Timer example ) throws SwfMetadataException;

  long countByWorkflowExecution( OwnerFullName ownerFullName,
                                 String domain,
                                 String runId ) throws SwfMetadataException;

  AbstractPersistentSupport<SimpleWorkflowMetadata.TimerMetadata,Timer,SwfMetadataException> withRetries( );

}
