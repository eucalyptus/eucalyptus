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
package com.eucalyptus.bootstrap;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;
import com.eucalyptus.bootstrap.Bootstrap.Stage;
import com.eucalyptus.configurable.ConfigurableClass;
import com.eucalyptus.configurable.ConfigurableField;
import com.eucalyptus.configurable.ConfigurableProperty;
import com.eucalyptus.event.PassiveEventListener;
import com.eucalyptus.system.Threads;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

@Provides( Component.jetty )
@RunDuring( Bootstrap.Stage.CloudServiceInit )
@DependsLocal( Component.eucalyptus )
@ConfigurableClass( root = "www", description = "Parameters controlling the web UI's http server." )
public class HttpServerBootstrapper extends Bootstrapper {  
  private static Logger LOG        = Logger.getLogger( HttpServerBootstrapper.class );
  @ConfigurableField( description = "Listen to HTTPs on this port.", initial = "" + 8443, changeListener = PortChangeListener.class, displayName="euca.https.port" )
  public static Integer HTTPS_PORT = 8443;
  @ConfigurableField( description = "Listen to HTTP on this port.", initial = "" + 8080, changeListener = PortChangeListener.class, displayName="euca.http.port" )
  public static Integer HTTP_PORT  = 8080;
  private static Server jettyServer;
  private static Thread serverThread;
  @ConfigurableField( initial = "", description = "Http Proxy Host" )
  public static String httpProxyHost;
  @ConfigurableField( initial = "", description = "Http Proxy Port" )
  public static String httpProxyPort;
 
  private static void setupJettyServer( ) throws Exception {
    //http proxy setup
	if(System.getProperty("http.proxyHost") != null) {
		httpProxyHost = System.getProperty("http.proxyHost");
	}
	if(System.getProperty("http.proxyPort") != null) {
		httpProxyPort = System.getProperty("http.proxyPort");
	} 
	jettyServer = new org.mortbay.jetty.Server( );
    System.setProperty( "euca.http.port", "" + HTTP_PORT );
    System.setProperty( "euca.https.port", "" + HTTPS_PORT );
    URL defaultConfig = ClassLoader.getSystemResource( "eucalyptus-jetty.xml" );
    XmlConfiguration jettyConfig = new XmlConfiguration( defaultConfig );
    jettyConfig.configure( jettyServer );
  }
  
  private static void startJettyServer( ) {
    serverThread = Threads.newThread( new Runnable( ) {
      @Override
      public void run( ) {
        try {
          jettyServer.start( );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }, "jetty" );
    serverThread.start( );
  }
  
  @Override
  public boolean load( Stage current ) throws Exception {
    setupJettyServer( );
    return true;
  }
  
  @Override
  public boolean start( ) throws Exception {
    LOG.info( "Starting admin interface." );
    startJettyServer( );
    return true;
  }
  public static class PortChangeListener extends PassiveEventListener<ConfigurableProperty> {
    @Override
    public void firingEvent( ConfigurableProperty t ) {
      LOG.info( "Change occurred to property " + t.getQualifiedName( ) + " which requires restarting the web server." );
      if ( jettyServer == null ) {
        return;
      } else if ( jettyServer.isRunning( ) ) {
        try {
          jettyServer.stop( );
          for ( int i = 0; i < 10 && !jettyServer.isStopped( ) && jettyServer.isStopping( ); i++ ) {
            try {
              TimeUnit.MILLISECONDS.sleep( 500 );
            } catch ( InterruptedException e ) {}
          }
          jettyServer.destroy( );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
        try {
          System.setProperty( t.getDisplayName( ), t.getValue( ) );
          setupJettyServer( );
          startJettyServer( );
        } catch ( Exception e ) {
          LOG.debug( e, e );
        }
      }
    }
  }

}
