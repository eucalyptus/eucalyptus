/*******************************************************************************
 *Copy) 2009  Eucalyptus Systems, Inc.
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javassist.Modifier;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.crypto.BaseProvider;
import com.eucalyptus.auth.crypto.CertificateProvider;
import com.eucalyptus.auth.crypto.CryptoProvider;
import com.eucalyptus.auth.crypto.HmacProvider;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.entities.EntityWrapper;
import edu.ucsb.eucalyptus.constants.EventType;
import edu.ucsb.eucalyptus.msgs.EventRecord;

public class Credentials {
  static String         DB_NAME = "eucalyptus_auth";
  public static Logger         LOG     = Logger.getLogger( Credentials.class );
  private static CryptoProvider cryptoProvider;
  private static CertificateProvider certProvider;
  private static HmacProvider hmacProvider;
  static {
    //TODO FIXME TODO BROKEN FAIL: discover this at bootstrap time.
    try {
      Class.forName( "com.eucalyptus.auth.crypto.DefaultCryptoProvider" );
    } catch ( ClassNotFoundException e ) {
    }
  }
  
  public static <T> EntityWrapper<T> getEntityWrapper( ) {
    return new EntityWrapper<T>( Credentials.DB_NAME );
  }
  private static BaseProvider DUMMY = new BaseProvider() {};
  private static ConcurrentMap<Class, BaseProvider> providers = new ConcurrentHashMap<Class, BaseProvider>( ){{    
    put( CertificateProvider.class, DUMMY );
    put( HmacProvider.class, DUMMY );
    put( CryptoProvider.class, DUMMY );
  }};
  
  public static CertificateProvider getCertificateProvider( ) {
    return (CertificateProvider) providers.get( CertificateProvider.class );
  }
  public static HmacProvider getHmacProvider( ) {
    return (HmacProvider) providers.get( HmacProvider.class );
  }
  public static CryptoProvider getCryptoProvider( ) {
    return (CryptoProvider) providers.get( CryptoProvider.class );
  }
  
  
  public static class CryptoProviderDiscovery extends ServiceJarDiscovery {
    public CryptoProviderDiscovery( ) {}
    @Override
    public Double getPriority( ) {
      return 0.01d;
    }
    @Override
    public boolean processsClass( Class candidate ) throws Throwable {
      if( BaseProvider.class.isAssignableFrom( candidate ) && !Modifier.isInterface( candidate.getModifiers( ) ) && !Modifier.isAbstract( candidate.getModifiers( ) ) ) {
        try {
          BaseProvider o = ( BaseProvider ) candidate.newInstance( );
          for( Class c : Credentials.providers.keySet( ) ) {
            if( c.isAssignableFrom( candidate ) ) {
              Object curr = Credentials.providers.get( c );
              if( curr == null ) {
                LOG.info( EventRecord.here( this.getClass( ), EventType.PROVIDER_CONFIGURED, CertificateProvider.class.toString( ), candidate.getCanonicalName( ) ) );
                Credentials.providers.put( c, o );
              } else if( !candidate.getSimpleName( ).startsWith( "Default" ) ) {
                LOG.info( EventRecord.here( this.getClass( ), EventType.PROVIDER_CONFLICT, CertificateProvider.class.getCanonicalName( ), curr.getClass( ).getCanonicalName( ), candidate.getCanonicalName( )  ) );
                Credentials.providers.put( c, o );
              } else {
                LOG.info( EventRecord.here( this.getClass( ), EventType.PROVIDER_IGNORED, CertificateProvider.class.toString( ) ) );
                return false;
              }
            }
          }
          return true;
        } catch ( Exception e ) {
          LOG.error( e, e );
          LOG.fatal( "Provider class " + candidate + " failed during <init>().  This must be fixed for the system to run." );
          System.exit( -1 );
        }
      }
      return false;
    }
  }
  
}
