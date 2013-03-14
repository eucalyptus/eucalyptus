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

package com.eucalyptus.component.auth;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.log4j.Logger;

import com.eucalyptus.auth.util.X509CertHelper;
import com.eucalyptus.bootstrap.Bootstrap;
import com.eucalyptus.bootstrap.Bootstrapper;
import com.eucalyptus.bootstrap.DependsLocal;
import com.eucalyptus.bootstrap.DependsRemote;
import com.eucalyptus.bootstrap.Provides;
import com.eucalyptus.bootstrap.RunDuring;
import com.eucalyptus.component.ComponentId;
import com.eucalyptus.component.ComponentIds;
import com.eucalyptus.component.NoSuchComponentException;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.component.id.HttpService;
import com.eucalyptus.crypto.Certs;
import com.eucalyptus.crypto.KeyStore;
import com.eucalyptus.crypto.util.AbstractKeyStore;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.PEMFiles;
import com.eucalyptus.empyrean.Empyrean;
import com.eucalyptus.records.EventRecord;
import com.eucalyptus.records.EventType;
import com.eucalyptus.system.SubDirectory;
import com.google.common.base.Predicate;

public class SystemCredentials {
  private static Logger                             LOG       = Logger.getLogger( SystemCredentials.class );
  private static ConcurrentMap<String, Credentials> providers = new ConcurrentHashMap<String, Credentials>( );
  
  public static <T extends ComponentId> Credentials lookup( ComponentId compId ) {
    if ( providers.containsKey( compId.name( ) ) ) {
      return providers.get( compId.name( ) );
    } else {
      try {
        return new Credentials( compId );
      } catch ( Exception ex ) {
        LOG.error( ex, ex );
        throw new RuntimeException( "Failed to lookup system credentials for: " + compId + " because: " + ex.getMessage( ), ex );
      }
    }
  }
  
  public static <T extends ComponentId> Credentials lookup( Class<T> compId ) {
    return lookup( ComponentIds.lookup( compId ) );
  }
  
  public static class Credentials {
    private final ComponentId     componentId;
    private final String          name;
    private final X509Certificate cert;
    private final KeyPair         keyPair;
    private final String					certFingerprint;
    
    private Credentials( ComponentId componentId ) throws Exception {
      this.componentId = componentId;
      this.name = componentId.name( );
      this.cert = loadCertificate( componentId );
      this.keyPair = loadKeyPair( componentId );
      this.certFingerprint = X509CertHelper.calcFingerprint(this.cert);
      EventRecord.here( SystemCredentials.class, EventType.COMPONENT_INFO, "initialized", this.name, this.cert.getSubjectDN( ).toString( ) ).info( );
      SystemCredentials.providers.put( this.name, this );
    }
    
    @Override
    public int hashCode( ) {
      final int prime = 31;
      int result = 1;
      result = prime * result + ( ( this.name == null )
        ? 0
        : this.name.hashCode( ) );
      return result;
    }
    
    @Override
    public boolean equals( Object obj ) {
      if ( this == obj ) {
        return true;
      }
      if ( obj == null ) {
        return false;
      }
      if ( getClass( ) != obj.getClass( ) ) {
        return false;
      }
      Credentials other = ( Credentials ) obj;
      if ( this.name == null ) {
        if ( other.name != null ) {
          return false;
        }
      } else if ( !this.name.equals( other.name ) ) {
        return false;
      }
      return true;
    }
    
    private KeyPair loadKeyPair( ComponentId componentId ) throws Exception {
      if ( this.componentId.hasCredentials( ) && EucaKeyStore.getInstance( ).containsEntry( this.name ) ) {
        try {
          EventRecord.here( SystemCredentials.class, EventType.COMPONENT_INFO, "initializing", this.name ).info( );
          return EucaKeyStore.getInstance( ).getKeyPair( this.name, this.name );
        } catch ( Exception e ) {
          LOG.fatal( "Failed to read keys from the keystore: " + componentId + ".  Please repair the keystore by hand." );
          LOG.fatal( e, e );
          throw e;
        }
      } else {
        throw new NoSuchComponentException( "Failed to find credentials for: " + componentId );
      }
    }
    
    private X509Certificate loadCertificate( ComponentId componentId ) throws Exception {
      if ( this.componentId.hasCredentials( ) && EucaKeyStore.getInstance( ).containsEntry( this.name ) ) {
        try {
          EventRecord.here( SystemCredentials.class, EventType.COMPONENT_INFO, "initializing", this.name ).info( );
          return EucaKeyStore.getInstance( ).getCertificate( this.name );
        } catch ( Exception e ) {
          LOG.fatal( "Failed to read certificate from the keystore: " + componentId + ".  Please repair the keystore by hand." );
          LOG.fatal( e, e );
          throw e;
        }
      } else {
        throw new NoSuchComponentException( "Failed to find credentials for: " + componentId );
      }
    }
    
    public String getPem( ) {
      return B64.url.encString( PEMFiles.getBytes( this.getCertificate( ) ) );
    }
    
    public X509Certificate getCertificate( ) {
      return this.cert;
    }
    
    public PrivateKey getPrivateKey( ) {
      return this.keyPair.getPrivate( );
    }
    
    public KeyPair getKeyPair( ) {
      return this.keyPair;
    }
    
    public String getCertFingerprint() {
    	return this.certFingerprint;
    }  	
    
  }
  
  static boolean checkKeystore( ComponentId name ) throws Exception {
    return EucaKeyStore.getCleanInstance( ).containsEntry( name.name( ) );
  }
  
  static boolean check( Class<? extends ComponentId> compIdType ) {
    return check( ComponentIds.lookup( compIdType ) );
  }
  
  static boolean check( ComponentId compId ) {
    if ( !compId.hasCredentials( ) ) {
      return true;
    } else {
      return EucaKeyStore.getInstance( ).containsEntry( compId.name( ) );
    }
  }
  
  private static Credentials create( ComponentId compId ) throws Exception {
    if ( !SystemCredentials.check( compId ) ) {
      try {
        KeyPair sysKp = Certs.generateKeyPair( );
        X509Certificate sysX509 = Certs.generateServiceCertificate( sysKp, compId.name( ) );
        if ( ComponentIds.lookup( Eucalyptus.class ).name( ).equals( compId.name( ) ) ) {
          PEMFiles.write( SubDirectory.KEYS.toString( ) + "/cloud-cert.pem", sysX509 );
          PEMFiles.write( SubDirectory.KEYS.toString( ) + "/cloud-pk.pem", sysKp.getPrivate( ) );
        }
        EucaKeyStore.getInstance( ).addKeyPair( compId.name( ), sysX509, sysKp.getPrivate( ), compId.name( ) );
        EucaKeyStore.getInstance( ).store( );
      } catch ( Exception e ) {
        throw e;
      }
    }
    return new Credentials( compId );
  }
  
  private static boolean checkAllKeys( ) {
    for ( ComponentId c : ComponentIds.list( ) ) {
      if ( !c.hasCredentials( ) ) {
        continue;
      } else {
        try {
          if ( !EucaKeyStore.getCleanInstance( ).containsEntry( c.name( ) ) ) {
            LOG.error( "Failed to lookup key for " + c.getCapitalizedName( ) + " with alias=" + c.name( ) + " in file "
                       + EucaKeyStore.getInstance( ).getFileName( ) );
            return false;
          }
        } catch ( Exception e ) {
          LOG.error( e, e );
          return false;
        }
      }
    }
    return true;
  }
  
  public static boolean initialize( ) {
    try {
      if ( !SystemCredentials.check( Eucalyptus.class ) ) {
        SystemCredentials.create( Eucalyptus.INSTANCE );
      }
      for ( ComponentId c : ComponentIds.list( ) ) {
        if ( !SystemCredentials.check( c ) ) {
          SystemCredentials.create( c );
        }
      }
    } catch ( Exception e ) {
      LOG.error( e, e );
      return false;
    }
    return true;
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.SystemCredentialsInit )
  @DependsLocal( Eucalyptus.class )
  public static class SystemCredentialBootstrapper extends Bootstrapper.Simple {
    @Override
    public boolean load( ) throws Exception {
      try {
        if ( !SystemCredentials.check( Eucalyptus.class ) ) {
          SystemCredentials.lookup( Eucalyptus.INSTANCE );
        }
        for ( ComponentId c : ComponentIds.list( ) ) {
          if ( !SystemCredentials.check( c ) ) {
            SystemCredentials.lookup( c );
          }
        }
      } catch ( Exception e ) {
        LOG.error( e, e );
        return false;
      }
      return true;
    }
    
  }
  
  @Provides( Empyrean.class )
  @RunDuring( Bootstrap.Stage.SystemCredentialsInit )
  @DependsRemote( Eucalyptus.class )
  public static class RemoteComponentCredentialBootstrapper extends Bootstrapper.Simple {
    
    @Override
    public boolean load( ) throws Exception {
      while ( !SystemCredentials.checkAllKeys( ) ) {
        LOG.fatal( "Waiting for system credentials before proceeding with startup..." );
        try {
          Thread.sleep( 2000 );
        } catch ( Exception e ) {
          Thread.currentThread( ).interrupt( );
        }
      }
      for ( ComponentId c : ComponentIds.list( ) ) {
        if ( c.hasCredentials( ) ) {
          LOG.info( "Initializing system credentials for " + c.name( ) );
          SystemCredentials.lookup( c );
        }
      }
      return true;
    }
    
  }
  
  private static final class EucaKeyStore extends AbstractKeyStore {
    public static String    FORMAT         = "pkcs12";
    private static String   KEY_STORE_PASS = "eucalyptus";
    private static String   FILENAME       = "euca.p12";
    
    /**
     * Enforces that keys are less than the MAX_SIZE of 4096. 
     */
    private enum RestrictSize implements Predicate<Key> {
      INSTANCE;
      private static final int MAX_SIZE = 4096;
      
      public boolean apply( Key arg0 ) {
        RSAKey key = ( ( RSAKey ) arg0 );
        if ( key.getModulus( ).bitLength( ) > MAX_SIZE ) {
          SecurityException ex = new SecurityException( "Illegal key size: " + key.getModulus( ).bitLength( ) + " > " + MAX_SIZE + " (max key size)" );
          LOG.trace( ex, ex );
          throw ex;
        } else {
          return true;
        }
      }
      
    }

    private static KeyStore singleton      = EucaKeyStore.getInstance( );
    
    public static KeyStore getInstance( ) {
      synchronized ( EucaKeyStore.class ) {
        if ( EucaKeyStore.singleton == null ) {
          try {
            singleton = new EucaKeyStore( );
          } catch ( final Exception e ) {
            LOG.error( e, e );
          }
        }
      }
      return EucaKeyStore.singleton;
    }
    
    public static KeyStore getCleanInstance( ) throws Exception {
      synchronized ( EucaKeyStore.class ) {
        singleton = new EucaKeyStore( );
      }
      return singleton;
    }
    
    private EucaKeyStore( ) throws GeneralSecurityException, IOException {
      super( SubDirectory.KEYS.toString( ) + File.separator + EucaKeyStore.FILENAME, EucaKeyStore.KEY_STORE_PASS, EucaKeyStore.FORMAT );
    }
    
    @Override
    public boolean check( ) throws GeneralSecurityException {
      return ( this.getCertificate( ComponentIds.lookup( HttpService.class ).name( ) ) != null )
             && ( this.getCertificate( ComponentIds.lookup( Eucalyptus.class ).name( ) ) != null );
    }

    @Override
    public final KeyPair getKeyPair( String alias, String password ) throws GeneralSecurityException {
      KeyPair keyPair = super.getKeyPair( alias, password );
      RestrictSize.INSTANCE.apply( keyPair.getPrivate( ) );
      RestrictSize.INSTANCE.apply( keyPair.getPublic( ) );
      return keyPair;
    }

    @Override
    public final X509Certificate getCertificate( String alias ) throws GeneralSecurityException {
      X509Certificate certificate = super.getCertificate( alias );
      RestrictSize.INSTANCE.apply( certificate.getPublicKey( ) );
      return certificate;
    }

    @Override
    public final Key getKey( String alias, String password ) throws GeneralSecurityException {
      Key key = super.getKey( alias, password );
      RestrictSize.INSTANCE.apply( key );
      return key;
    }

    @Override
    public final void addCertificate( String alias, X509Certificate cert ) throws IOException, GeneralSecurityException {
      RestrictSize.INSTANCE.apply( cert.getPublicKey( ) );
      super.addCertificate( alias, cert );
    }

    @Override
    public final void addKeyPair( String alias, X509Certificate cert, PrivateKey privateKey, String keyPassword ) throws IOException, GeneralSecurityException {
      RestrictSize.INSTANCE.apply( cert.getPublicKey( ) );
      RestrictSize.INSTANCE.apply( privateKey );
      super.addKeyPair( alias, cert, privateKey, keyPassword );
    }
  }
  
  public static final KeyStore getKeyStore( ) {
    return EucaKeyStore.getInstance( );
  }
  
}
