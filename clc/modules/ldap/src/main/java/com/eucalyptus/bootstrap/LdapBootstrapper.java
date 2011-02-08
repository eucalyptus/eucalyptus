/*******************************************************************************
 *Copyright (c) 2009 Eucalyptus Systems, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, only version 3 of the License.
 * 
 * 
 * This file is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Please contact Eucalyptus Systems, Inc., 130 Castilian
 * Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 * if you need additional information or have any questions.
 * 
 * This file may incorporate work covered under the following copyright and
 * permission notice:
 * 
 * Software License Agreement (BSD License)
 * 
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use of this software in source and binary forms, with
 * or without modification, are permitted provided that the following
 * conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 * THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 * LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 * SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 * IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 * BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 * THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 * OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 * WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 * ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package com.eucalyptus.bootstrap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.component.id.Any;
import com.eucalyptus.component.id.Eucalyptus;
import com.eucalyptus.ldap.LdapConfiguration;

@RunDuring( Bootstrap.Stage.DatabaseInit )
@Provides( Any.class )
@DependsLocal( Eucalyptus.class )
public class LdapBootstrapper extends Bootstrapper implements DatabaseBootstrapper {
  private static Logger               LOG         = Logger.getLogger( LdapBootstrapper.class );
  private static Bootstrapper singleton;
  private static final SlapdResource  slapd       = new SlapdResource( );
  
  public static Bootstrapper getInstance( ) {
    synchronized ( LdapBootstrapper.class ) {
      if ( singleton == null ) {
        singleton = new LdapBootstrapper( );
      }
    }
    return singleton;
  }
  
  private LdapBootstrapper( ) {}
  
  private void startServiceResource( ) throws IOException {
    this.getResource( ).initialize( );
    try {
      this.getResource( ).start( "eucalyptus-openldap", SERVICE_OPTIONS );
    } catch ( Exception e ) {
      LOG.debug( e, e );
      LOG.fatal( "Failed to initialize " + slapd.getClass( ).getSimpleName( ) + " options." );
      System.exit( 1 );
    }
    if ( !this.getResource( ).isRunning( ) ) {
      LOG.fatal( "Failed to start database." );
      System.exit( 1 );
    }
  }
  
  private Map<String, String> SERVICE_OPTIONS = new HashMap<String, String>( ) {
                                                {
                                                  put( "-u", System.getProperty( "euca.user" ) );
                                                  put( "-n", "eucalyptus-slapd" );
                                                  put( "-f", SlapdResource.CONFIG_FILE );
                                                  put( "-h", "ldap://0.0.0.0:8778/" );
                                                  put( "-d", "Any" );
                                                }
                                              };
  
  @Override
  public boolean load( ) throws Exception {
    if ( LdapConfiguration.ENABLE_LDAP ) {
      try {
        LOG.debug( "Initializing SSL just in case: " + Class.forName( "com.eucalyptus.auth.util.SslSetup" ) );
        LOG.debug( "Initializing db password: " + Class.forName( "com.eucalyptus.auth.util.Hashes" ) );
      } catch ( Throwable t ) {}
      this.startServiceResource( );
    }
    return true;
  }
  
  public boolean isRunning( ) {
    return this.getResource( ).isRunning( ) && this.getResource( ).isReady( );
  }
  
  public void hup( ) {
    this.getResource( ).shutdown( );
    try {
      this.startServiceResource( );
    } catch ( IOException e ) {
      LOG.debug( e, e );
      throw new RuntimeException( e );
    }
  }
  
  public SlapdResource getResource() {
    return slapd;
  }
  
  @Override
  public boolean start( ) throws Exception {
    return true;
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
  public void destroy( ) throws Exception {}

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
    return true;
  }
  
}
