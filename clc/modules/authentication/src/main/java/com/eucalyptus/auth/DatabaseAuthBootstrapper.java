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
import java.util.ServiceLoader;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.ldap.LdapSync;
import com.eucalyptus.auth.policy.PolicyEngineImpl;
import com.eucalyptus.auth.principal.Account;
import com.eucalyptus.auth.principal.Role;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.system.Threads;
import com.eucalyptus.util.RestrictedTypes;
import com.google.common.collect.Lists;

@Provides( Empyrean.class )
@RunDuring( Bootstrap.Stage.UserCredentialsInit )
public class DatabaseAuthBootstrapper extends Bootstrapper {
  private static Logger LOG = Logger.getLogger( DatabaseAuthBootstrapper.class );
    
  public boolean load( ) throws Exception {
  	DatabaseAuthProvider dbAuth = new DatabaseAuthProvider( );
  	Accounts.setAccountProvider( dbAuth );
  	Permissions.setPolicyEngine( new PolicyEngineImpl( ) );
    return true;
  }
  
  public boolean start( ) throws Exception {
    if(ComponentIds.lookup( Eucalyptus.class ).isAvailableLocally()) {
      this.ensureSystemAdminExists( );
      this.ensureSystemRolesExist( );
      // User info map key is case insensitive.
      // Older code may produce non-lowercase keys.
      // Normalize them if there is any.
      this.ensureUserInfoNormalized( );
      // EUCA-9376 - Workaround to avoid multiple admin users in the blockstorage account due to EUCA-9635  
      this.ensureBlockStorageAccountExists();
      LdapSync.start( );
    }
    return true;
  }
  
  private void ensureUserInfoNormalized() {
    try {
      Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      User sysadmin = account.lookupUserByName( User.ACCOUNT_ADMIN );
      if ( sysadmin.getInfo( ).containsKey( "Email" ) ) {
        Threads.newThread( new Runnable( ) {

          @Override
          public void run() {
            try {
              LOG.debug( "Starting to normalize user info for all users" );
              Accounts.normalizeUserInfo( );
            } catch ( Exception e ) {
              LOG.error( e, e );
            }
          }
          
        } ).start( ); 
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
    }
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#enable()
   */
  @Override
  public boolean enable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#stop()
   */
  @Override
  public boolean stop( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#destroy()
   */
  @Override
  public void destroy( ) throws Exception {
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#disable()
   */
  @Override
  public boolean disable( ) throws Exception {
    return true;
  }

  /**
   * @see com.eucalyptus.bootstrap.Bootstrapper#check()
   */
  @Override
  public boolean check( ) throws Exception {
    return LdapSync.check( );
  }
  
  private void ensureSystemAdminExists( ) throws Exception {
    try {
      Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      account.lookupUserByName( User.ACCOUNT_ADMIN );
    } catch ( Exception e ) {
      LOG.debug( "System admin does not exist. Adding it now." );
      // Order matters.
      try {
        Account system = Accounts.addSystemAccount( );
        User admin = system.addUser( User.ACCOUNT_ADMIN, "/", true, null );
        admin.createKey( );
      } catch ( Exception ex ) {
        LOG.error( ex , ex );
      }
    }
  }

  private void ensureSystemRolesExist( ) throws Exception {
    try {
      final Account account = Accounts.lookupAccountByName( Account.SYSTEM_ACCOUNT );
      final List<Role> roles = account.getRoles( );
      final List<String> roleNames = Lists.transform( roles, RestrictedTypes.toDisplayName( ) );
      for ( final SystemRoleProvider provider : ServiceLoader.load( SystemRoleProvider.class ) ) {
        if ( !roleNames.contains( provider.getName( ) ) ) {
          addSystemRole( account, provider );
        }
      }
    } catch ( Exception e ) {
      LOG.error( "Error checking system roles.", e );
    }
  }

  private void addSystemRole( final Account account,
                              final SystemRoleProvider provider ) {
    LOG.info( String.format( "Creating system role: %s", provider.getName( ) ) );
    try {
      final String name = provider.getName( );
      final String path = provider.getPath( );
      final String assumeRolePolicy = provider.getAssumeRolePolicy( );
      final String policy = provider.getPolicy( );
      final Role role = account.addRole( name, path, assumeRolePolicy );
      role.addPolicy( name, policy );
    } catch ( Exception e ) {
      LOG.error( String.format( "Error adding system role: %s", provider.getName( ) ), e );
    }
  }

  public interface SystemRoleProvider {
    String getName();
    String getPath();
    String getAssumeRolePolicy();
    String getPolicy();
  }
  
  // EUCA-9376 - Workaround to avoid multiple admin users in the blockstorage account due to EUCA-9635  
  private void ensureBlockStorageAccountExists( ) throws Exception {
    try {
      Accounts.lookupAccountByName( Account.BLOCKSTORAGE_SYSTEM_ACCOUNT );
    } catch ( Exception e ) {
      try {
    	Accounts.addSystemAccountWithAdmin( Account.BLOCKSTORAGE_SYSTEM_ACCOUNT ); 
      } catch (Exception e1) {
    	LOG.error("Error during account creation for " + Account.BLOCKSTORAGE_SYSTEM_ACCOUNT, e1);
      }
    }
  }
}
