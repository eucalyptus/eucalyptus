/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
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

package com.eucalyptus.auth;

import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.entities.PolicyEntity;
import com.eucalyptus.auth.principal.Group;
import com.eucalyptus.auth.principal.Policy;
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
  public String getPolicyId( ) {
    return this.delegate.getPolicyId( );
  }
  
  @Override
  public String getName( ) {
    return this.delegate.getName( );
  }
  
  @Override
  public String getText( ) {
    return this.delegate.getText( );
  }
  
  @Override
  public String getVersion( ) {
    return this.delegate.getPolicyVersion( );
  }
  
  @Override
  public Group getGroup( ) throws AuthException {
    final List<Group> results = Lists.newArrayList( );
    try {
      Transactions.one( PolicyEntity.newInstanceWithId( this.delegate.getPolicyId( ) ), new Tx<PolicyEntity>( ) {
        public void fire( PolicyEntity t ) {
          results.add( new DatabaseGroupProxy( t.getGroup( ) ) );
        }
      } );
    } catch ( ExecutionException e ) {
      Debugging.logError( LOG, e, "Failed to setName for " + this.delegate );
      throw new AuthException( e );
    }
    return results.get( 0 );
  }
  
}
