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

import static com.eucalyptus.simpleworkflow.SimpleWorkflowConfiguration.getDeprecatedActivityTypeRetentionDurationMillis;
import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTypeMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.ActivityType;
import com.eucalyptus.simpleworkflow.ActivityTypes;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.util.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceActivityTypes extends SwfPersistenceSupport<ActivityTypeMetadata,ActivityType> implements ActivityTypes {

  public PersistenceActivityTypes( ) {
    super( "activity-type" );
  }

  public long countByDomain( final OwnerFullName ownerFullName,
                             final String domain ) throws SwfMetadataException {
    return countByExample(
        ActivityType.exampleWithOwner( ownerFullName ),
        Restrictions.eq( "domain.displayName", domain ),
        Collections.singletonMap( "domain", "domain" ) );
  }

  public <T> List<T> listDeprecatedExpired( final long time,
                                            final Function<? super ActivityType,T> transform ) throws SwfMetadataException {
    return listByExample(
        ActivityType.exampleWithOwner( null ),
        Predicates.alwaysTrue( ),
        Restrictions.lt( "deprecationTimestamp", new Date( time - getDeprecatedActivityTypeRetentionDurationMillis( ) ) ),
        Collections.<String, String>emptyMap(),
        transform );
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
