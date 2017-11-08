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

import static com.eucalyptus.simpleworkflow.SimpleWorkflowProperties.getDeprecatedWorkflowTypeRetentionDurationMillis;
import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.WorkflowTypeMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.simpleworkflow.WorkflowTypes;
import com.eucalyptus.simpleworkflow.WorkflowType;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceWorkflowTypes extends SwfPersistenceSupport<WorkflowTypeMetadata,WorkflowType> implements WorkflowTypes {

  public PersistenceWorkflowTypes( ) {
    super( "workflow-type" );
  }

  public long countByDomain( final OwnerFullName ownerFullName,
                             final String domain ) throws SwfMetadataException {
    return countByExample(
        WorkflowType.exampleWithOwner( ownerFullName ),
        Restrictions.eq( "domain.displayName", domain ),
        Collections.singletonMap( "domain", "domain" ) );
  }

  public <T> List<T> listDeprecatedExpired( final long time,
                                            final Function<? super WorkflowType,T> transform ) throws SwfMetadataException {
    return listByExample(
        WorkflowType.exampleWithOwner( null ),
        Predicates.alwaysTrue(),
        Restrictions.conjunction( )
            .add( Restrictions.isEmpty( "executions" ) )
            .add( Restrictions.lt( "deprecationTimestamp", new Date( time - getDeprecatedWorkflowTypeRetentionDurationMillis( ) ) ) ),
        Collections.<String, String>emptyMap(),
        transform );
  }

  @Override
  protected WorkflowType exampleWithOwner( final OwnerFullName ownerFullName ) {
    return WorkflowType.exampleWithOwner( ownerFullName );
  }

  @Override
  protected WorkflowType exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return WorkflowType.exampleWithName( ownerFullName, name );
  }

}
