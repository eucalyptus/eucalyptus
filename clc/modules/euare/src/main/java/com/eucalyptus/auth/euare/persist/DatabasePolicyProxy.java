/*************************************************************************
 * Copyright 2008 Regents of the University of California
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
 * POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 * THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 * COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 * AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 * SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 * WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 * REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 * IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 * NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.euare.persist;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Debugging;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity;
import com.eucalyptus.auth.euare.persist.entities.PolicyEntity_;
import com.eucalyptus.auth.euare.principal.EuareGroup;
import com.eucalyptus.auth.principal.Policy;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.EntityRestriction;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;

public class DatabasePolicyProxy implements Policy {

  private static final long serialVersionUID = 1L;
  
  private static Logger LOG = Logger.getLogger( DatabasePolicyProxy.class );
                                               
  private PolicyEntity delegate;
  
  public DatabasePolicyProxy( PolicyEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String getText( ) {
    return this.delegate.getText( );
  }
  
  public String getVersion( ) {
    return this.delegate.getPolicyVersion( );
  }

  public Integer getPolicyVersion( ) {
    return this.delegate.getVersion( );
  }

  public EuareGroup getGroup( ) throws AuthException {
    final List<EuareGroup> results = Lists.newArrayList( );
    try {
      final EntityRestriction<PolicyEntity> restriction = Entities.restriction( PolicyEntity.class )
          .equal( PolicyEntity_.id, this.delegate.getPolicyId( ) )
          .build( );
      Transactions.one( restriction, new Tx<PolicyEntity>( ) {
        public void fire( PolicyEntity t ) {
          results.add( new DatabaseGroupProxy( t.getGroup( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroup for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }
}
