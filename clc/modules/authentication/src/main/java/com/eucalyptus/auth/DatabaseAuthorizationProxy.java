/*************************************************************************
 * Copyright 2009-2013 Eucalyptus Systems, Inc.
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
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth;

import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.AuthorizationEntity;
import com.eucalyptus.auth.entities.ConditionEntity;
import com.eucalyptus.auth.principal.Authorization;
import com.eucalyptus.auth.principal.Condition;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Principal;
import com.eucalyptus.entities.Transactions;
import java.util.concurrent.ExecutionException;
import com.eucalyptus.util.Tx;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class DatabaseAuthorizationProxy implements Authorization {
  
  private static final long serialVersionUID = 1L;

  private static Logger LOG = Logger.getLogger( DatabaseAuthorizationProxy.class );

  private AuthorizationEntity delegate;
  
  public DatabaseAuthorizationProxy( AuthorizationEntity delegate ) {
    this.delegate = delegate;
  }
  
  @Override
  public EffectType getEffect( ) {
    return this.delegate.getEffect( );
  }

  @Override
  public List<Condition> getConditions( ) {
    final List<Condition> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          for ( ConditionEntity c : t.getStatement( ).getConditions( ) ) {
            results.add( new DatabaseConditionProxy( c ) );
          }
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getConditions for " + this.delegate );
    }
    return results;
  }

  @Override
  public String toString( ) {
    final StringBuilder sb = new StringBuilder( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          sb.append( t.toString( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to toString for " + this.delegate );
    }
    return sb.toString( );
  }

  @Override
  public Boolean isNotAction( ) {
    return this.delegate.isNotAction( );
  }

  @Override
  public Boolean isNotResource( ) {
    return this.delegate.isNotResource( );
  }

  @Override
  public String getType( ) {
    return this.delegate.getType( );
  }

  @Override
  public Set<String> getActions( ) {
    final Set<String> results = Sets.newHashSet( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.addAll( t.getActions( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getActions for " + this.delegate );
    }
    return results;
  }

  @Override
  public Set<String> getResources( ) {
    final Set<String> results = Sets.newHashSet( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.addAll( t.getResources( ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getResources for " + this.delegate );
    }
    return results;
  }

  @Override
  public Group getGroup( ) {
    final List<Group> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        public void fire( AuthorizationEntity t ) {
          results.add( new DatabaseGroupProxy( t.getStatement( ).getPolicy( ).getGroup( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getGroup for " + this.delegate );
    }
    return results.get( 0 );
  }

  @Override
  public Principal getPrincipal( ) {
    final List<Principal> results = Lists.newArrayList( );
    try {
      Transactions.one( AuthorizationEntity.newInstanceWithId( this.delegate.getAuthorizationId() ), new Tx<AuthorizationEntity>( ) {
        @Override
        public void fire( AuthorizationEntity authorizationEntity ) {
          results.add( new DatabasePrincipalProxy( authorizationEntity.getStatement().getPrincipal() ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to getPrincipal for " + this.delegate );
    }
    return results.get( 0 );
  }

}
