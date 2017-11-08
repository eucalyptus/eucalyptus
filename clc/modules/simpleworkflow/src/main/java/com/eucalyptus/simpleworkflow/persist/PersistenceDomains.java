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

import static com.eucalyptus.simpleworkflow.SimpleWorkflowProperties.getDeprecatedDomainRetentionDurationMillis;
import static com.eucalyptus.simpleworkflow.common.SimpleWorkflowMetadata.DomainMetadata;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.hibernate.criterion.Restrictions;
import com.eucalyptus.component.annotation.ComponentNamed;
import com.eucalyptus.simpleworkflow.Domain;
import com.eucalyptus.simpleworkflow.Domains;
import com.eucalyptus.simpleworkflow.SwfMetadataException;
import com.eucalyptus.auth.principal.OwnerFullName;
import com.google.common.base.Function;
import com.google.common.base.Predicates;

/**
 *
 */
@ComponentNamed
public class PersistenceDomains extends SwfPersistenceSupport<DomainMetadata,Domain> implements Domains {

  public PersistenceDomains( ) {
    super( "domain" );
  }

  public <T> List<T> listDeprecatedExpired( final long time,
                                            final Function<? super Domain,T> transform ) throws SwfMetadataException {
    return listByExample(
        Domain.exampleWithStatus( Domain.Status.Deprecated ),
        Predicates.alwaysTrue( ),
        Restrictions.conjunction( )
            .add( Restrictions.isEmpty( "workflowTypes" ) )
            .add( Restrictions.lt( "lastUpdateTimestamp", new Date( time - getDeprecatedDomainRetentionDurationMillis( ) ) ) ),
        Collections.<String, String>emptyMap(),
        transform );
  }

  @Override
  protected Domain exampleWithOwner( final OwnerFullName ownerFullName ) {
    return Domain.exampleWithOwner( ownerFullName );
  }

  @Override
  protected Domain exampleWithName( final OwnerFullName ownerFullName, final String name ) {
    return Domain.exampleWithName( ownerFullName, name );
  }
}
