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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.Timer;
import com.eucalyptus.simpleworkflow.Timers;
import com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceTimers extends SwfPersistenceSupport<SimpleWorkflowMetadata.TimerMetadata,Timer> implements Timers {

  public PersistenceTimers( ) {
    super( "timer" );
  }

  public <T> List<T> listFired( final Function<? super Timer,T> transform ) throws SwfMetadataException {
    return listByExample(
        Timer.exampleWithOwner( null ),
        Predicates.alwaysTrue(),
        Restrictions.lt( "timeoutTimestamp", new Date() ),
        Collections.<String,String>emptyMap(),
        transform );
  }

  @Override
  protected Timer exampleWithOwner( final OwnerFullName ownerFullName ) {
    return Timer.exampleWithOwner( ownerFullName );
  }

  @Override
  protected Timer exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return Timer.exampleWithTimerId( ownerFullName, null, null, name );
  }
}
