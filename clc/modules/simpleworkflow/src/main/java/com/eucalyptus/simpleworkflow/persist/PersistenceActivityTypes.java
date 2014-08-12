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

import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTypeMetadata;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.ActivityType;
import com.eucalyptus.simpleworkflow.ActivityTypes;
import com.eucalyptus.util.OwnerFullName;

/**
 *
 */
@ComponentNamed
public class PersistenceActivityTypes extends SwfPersistenceSupport<ActivityTypeMetadata,ActivityType> implements ActivityTypes {

  public PersistenceActivityTypes( ) {
    super( "activity-type" );
  }

  @Override
  protected ActivityType exampleWithOwner( final OwnerFullName ownerFullName ) {
    return ActivityType.exampleWithOwner( ownerFullName );
  }

  @Override
  protected ActivityType exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return ActivityType.exampleWithName( ownerFullName, name );
  }
}
