/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
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
*    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.auth;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.Certs;
import com.eucalyptus.auth.util.EucaKeyStore;
import com.eucalyptus.auth.util.PEMFiles;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.system.SubDirectory;

@Provides( Component.any )
@RunDuring( Bootstrap.Stage.SystemCredentialsInit )
@DependsLocal( Component.eucalyptus )
public class SystemCredentialProvider extends Bootstrapper {
  private static Logger                                    LOG      = Logger.getLogger( SystemCredentialProvider.class );
  private static ConcurrentMap<Component, X509Certificate> certs    = new ConcurrentHashMap<Component, X509Certificate>( );
  private static ConcurrentMap<Component, KeyPair>         keypairs = new ConcurrentHashMap<Component, KeyPair>( );
  private Component                                        name;

  public SystemCredentialProvider( ) {
  }

  private SystemCredentialProvider( Component name ) {
    this.name = name;
  }

  public static SystemCredentialProvider getCredentialProvider( Component name ) {
    return new SystemCredentialProvider( name );
  }

  public X509Certificate getCertificate( ) {
    return SystemCredentialProvider.certs.get( this.name );
  }

  public PrivateKey getPrivateKey( ) {
    return SystemCredentialProvider.keypairs.get( this.name ).getPrivate( );
  }

  public KeyPair getKeyPair( ) {
    return SystemCredentialProvider.keypairs.get( this.name );
  }

  static void init( Component name ) throws Exception {
    new SystemCredentialProvider( name ).init( );
  }

  private void init( ) throws Exception {
    if ( EucaKeyStore.getInstance( ).containsEntry( this.name.name( ) ) ) {
      try {
        SystemCredentialProvider.certs.put( this.name, EucaKeyStore.getInstance( ).getCertificate( this.name.name( ) ) );
        SystemCredentialProvider.keypairs.put( this.name, EucaKeyStore.getInstance( ).getKeyPair( this.name.name( ), this.name.name( ) ) );
        return;
      } catch ( Exception e ) {
        SystemCredentialProvider.certs.remove( this );
        SystemCredentialProvider.keypairs.remove( this );
        LOG.fatal( "Failed to read keys from the keystore.  Please repair the keystore by hand." );
        LOG.fatal( e, e );
        throw e;
      }
    } else if ( Component.eucalyptus.isLocal( ) ) {
      this.createSystemCredentialProviderKey( this.name );
      return;
    }
    throw new RuntimeException( "Failed to load credentials because of an unknown error." );
  }

  static boolean checkKeystore( Component name ) throws Exception {
    return EucaKeyStore.getCleanInstance( ).containsEntry( name.name( ) );
  }

  static boolean check( Component name ) {
    return ( SystemCredentialProvider.keypairs.containsKey( name.name( ) ) && SystemCredentialProvider.certs.containsKey( name.name( ) ) ) && EucaKeyStore.getInstance( ).containsEntry( name.name( ) );
  }

  private void loadSystemCredentialProviderKey( String name ) throws Exception {
    Component alias = Component.valueOf( name );
    if ( this.certs.containsKey( alias ) ) {
      return;
    } else {
      createSystemCredentialProviderKey( alias );
    }
  }

  private void createSystemCredentialProviderKey( Component name ) throws Exception {
    try {
      KeyPair sysKp = Certs.generateKeyPair( );
      X509Certificate sysX509 = Certs.generateServiceCertificate( sysKp, name.name( ) );
      if( Component.eucalyptus.equals( name ) ) {
        PEMFiles.write( SubDirectory.KEYS.toString( ) + "/cloud-cert.pem", sysX509 );
        PEMFiles.write( SubDirectory.KEYS.toString( ) + "/cloud-pk.pem", sysKp.getPrivate( ) );
      }
      SystemCredentialProvider.certs.put( name, sysX509 );
      SystemCredentialProvider.keypairs.put( name, sysKp );
      // TODO: might need separate keystore for euca/hsqldb/ssl/jetty/etc.
      EucaKeyStore.getInstance( ).addKeyPair( name.name( ), sysX509, sysKp.getPrivate( ), name.name( ) );
      EucaKeyStore.getInstance( ).store( );
    } catch ( Exception e ) {
      SystemCredentialProvider.certs.remove( name );
      SystemCredentialProvider.keypairs.remove( name );
      EucaKeyStore.getInstance( ).remove( );
      throw e;
    }
  }

  @Override
  public boolean load( Stage current ) throws Exception {
    try {
      if ( !SystemCredentialProvider.check( Component.eucalyptus ) ) {
        SystemCredentialProvider.init( Component.eucalyptus );
      }
      for ( Component c : Component.values( ) ) {
        try {
          if ( !SystemCredentialProvider.check( c ) ) {
            SystemCredentialProvider.init( c );
          }
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      return false;
    }
    return true;
  }

  @Override
  public boolean start( ) throws Exception {
    return true;
  }
}
