/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util;

import java.util.List;
import java.util.NoSuchElementException;
import javax.persistence.PersistenceException;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.Permissions;
import com.eucalyptus.auth.policy.PolicyAnnotationRegistry;
import com.eucalyptus.auth.policy.PolicyResourceType;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.context.Context;
import com.eucalyptus.context.Contexts;
import com.eucalyptus.context.IllegalContextAccessException;
import edu.ucsb.eucalyptus.msgs.BaseMessage;

public class Lookups {
  private static Logger LOG = Logger.getLogger( Lookups.class );
  /**
   * Uses the provided {@code lookupFunction} to resolve the {@code identifier} to the underlying
   * object {@code T} with privileges determined by the current messaging context.
   * 
   * @param <T> type of object which needs looking up
   * @param identifier identifier of the desired object
   * @param lookupFunction class which resolves string identifiers to the underlying object
   * @return the object corresponding with the given {@code identifier}
   * @throws AuthException if the user is not authorized
   * @throws PersistenceException if an error occurred in the underlying retrieval mechanism
   * @throws NoSuchElementException if the requested {@code identifier} does not exist and the user is authorized.
   * @throws IllegalContextAccessException if the current request context cannot be determined.
   */
  public static <T extends HasOwningAccount> T doPrivileged( String identifier, Lookup<T> lookupFunction ) throws AuthException, IllegalContextAccessException, NoSuchElementException, PersistenceException {
    LOG.debug( "Attempting to lookup " + identifier + " using lookup: " + lookupFunction + " typed as " + Classes.genericsToClasses( lookupFunction ) );
    List<Class> lookupTypes = Classes.genericsToClasses( lookupFunction );
    if( lookupTypes.isEmpty( ) ) {
      throw new IllegalArgumentException( "Failed to find required generic type for lookup " + lookupFunction.getClass( ) + " so the policy type for looking up " + identifier + " cannot be determined." );
    } else {
      PolicyResourceType type = PolicyAnnotationRegistry.extractResourceType( lookupTypes.get( 0 ) );
      Context ctx = Contexts.lookup( );
      User requestUser = ctx.getUser( );
      String action = PolicySpec.requestToAction( ctx.getRequest( ) );

      try {
        T requestedObject = lookupFunction.lookup( identifier );
        if( requestedObject == null ) {
          throw new NoSuchElementException( "Failed to lookup requested " + type + " with id " + identifier + " using " + lookupFunction.getClass( ) ); 
        }
        Account owningAccount = Accounts.lookupUserById( requestedObject.getOwner( ).getUniqueId( ) ).getAccount( );
        if( !Permissions.isAuthorized( type.vendor( ), type.resource( ), identifier, owningAccount, action, requestUser ) ) {
          throw new AuthException( "Not authorized to use " + type.resource( ) + " identified by " + identifier + " as the user " + requestUser.getName( ) );
        }
        return requestedObject;
      } catch ( NoSuchElementException ex ) {
        throw ex;
      } catch ( AuthException ex ) {
        throw ex;
      } catch ( Throwable ex ) {
        throw new PersistenceException( "Error occurred while attempting to lookup " + identifier + " using lookup: " + lookupFunction + " typed as " + Classes.genericsToClasses( lookupFunction ) );
      }
    }
  }
  
  public static boolean checkPrivilege( BaseMessage request, String vendor, String resourceType, String resourceId, FullName resourceOwner ) {
    Context ctx = Contexts.lookup( );
    String action = PolicySpec.requestToAction( request );
    User requestUser = ctx.getUser( );
    Account account = null;
    try {
      account = Accounts.lookupUserById( resourceOwner.getUniqueId( ) ).getAccount( );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      return false;
    }
    return ( ctx.hasAdministrativePrivileges( ) ||
             Permissions.isAuthorized( vendor, resourceType, resourceId, account, action, requestUser ));
  }
  
  public static boolean checkPrivilege( String action, String vendor, String resourceType, String resourceId, String resourceOwnerAccountId ) {
    Context ctx = Contexts.lookup( );
    User requestUser = ctx.getUser( );
    Account account = null;
    try {
      account = Accounts.lookupAccountById( resourceOwnerAccountId );
    } catch ( AuthException e ) {
      LOG.error( e, e );
      return false;
    }
    return Permissions.isAuthorized( vendor, resourceType, resourceId, account, action, requestUser );
  }
  
}
