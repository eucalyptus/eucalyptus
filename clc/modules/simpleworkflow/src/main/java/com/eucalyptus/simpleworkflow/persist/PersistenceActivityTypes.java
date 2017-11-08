/*************************************************************************
 * Copyright 2009-2015 Ent. Services Development Corporation LP
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
package com.eucalyptus.simpleworkflow.persist;

import static com.eucalyptus.simpleworkflow.SimpleWorkflowProperties.getDeprecatedActivityTypeRetentionDurationMillis;
import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.ActivityTypeMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.ActivityType;
import com.eucalyptus.simpleworkflow.ActivityTypes;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.auth.principal.OwnerFullName;
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
