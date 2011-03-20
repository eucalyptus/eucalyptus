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
 *    THE REGENTS’ DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.util;

import groovy.jmx.builder.JmxBuilder;
import groovy.util.GroovyMBean;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.scripting.groovy.GroovyUtil;

public class Mbeans {
  private static final Map<String, String> EMPTY    = new HashMap<String, String>( );
  private static Logger                    LOG      = Logger.getLogger( Mbeans.class );
  private static final int                 JMX_PORT = 1099;//TODO:GRZE: configurable
//private static final int                 JMX_PORT = 8772;
  private static final String              JMX_HOST = "localhost";//TODO:GRZE: configurable
  private static final String              URI      = "service:jmx:rmi:///jndi/rmi://" + JMX_HOST + ":" + JMX_PORT + "/eucalyptus";
  private static MBeanServer               mbeanServer;
  private static JMXConnectorServer        jmxServer;
  private static Registry                  rmiRegistry;
  private static JmxBuilder                jmxBuilder;
  private static Map                       jmxProps = new HashMap( ) {
                                                      {
                                                        put( RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true" );
                                                      }
                                                    };
  
  public static void init( ) {////TODO:GRZE: make it a bootstrapper
    System.setSecurityManager( new RMISecurityManager( ) );
    System.setProperty( "euca.jmx.uri", URI );
    mbeanServer = ManagementFactory.getPlatformMBeanServer( ); //MBeanServerFactory.createMBeanServer( "com.eucalyptus" );
    
    try {
      try {
        rmiRegistry = LocateRegistry.createRegistry( JMX_PORT );
      } catch ( ExportException ex1 ) {
        LOG.error( ex1, ex1 );
        rmiRegistry = LocateRegistry.getRegistry( JMX_PORT );
      }
    } catch ( RemoteException ex1 ) {
      LOG.error( ex1, ex1 );
      throw BootstrapException.throwFatal( ex1.getMessage( ), ex1 );
    }
    try {
      jmxServer = JMXConnectorServerFactory.newJMXConnectorServer( new JMXServiceURL( URI ), jmxProps, mbeanServer );
      jmxServer.start( );
      jmxBuilder = new JmxBuilder( /*mbeanServer*/);
      jmxBuilder.setDefaultJmxNameDomain( "com.eucalyptus" );
//      jmxBuilder.setMBeanServer( mbeanServer );
    } catch ( MalformedURLException ex ) {
      LOG.error( ex, ex );
    } catch ( IOException ex ) {
      LOG.error( ex, ex );
    }
  }
  
  private static final MBeanServer mbeanServer( ) {
    return mbeanServer;
  }
  
  public static void register( final Object obj ) {
    if( obj.getClass( ).isAnonymousClass( ) ) {
      throw Exceptions.uncatchable( "MBeans.register(Object) only supports the registration of concrete classes, your argument is anonymous: " + obj.getClass( ).getName( ) );
    }
    String defaultExport = "bean( " +
        " target: obj, " +
        " name: \"${(obj.class.package.name}:type=${obj.getClass().getSimpleName()},\"," +
    		" )";
    //TODO:GRZE:load class specific config here
    try {
      List<GroovyMBean> mbeans = ( List<GroovyMBean> ) GroovyUtil.eval( "jmx.export{ " + defaultExport + "}", new HashMap( ) {
        {
          put( "jmx", jmxBuilder );
          put( "obj", obj );
        }
      } );
      for ( GroovyMBean mbean : mbeans ) {
        LOG.info( "MBean server: default=" + mbean.server( ).getDefaultDomain( ) + " all=" + Arrays.asList( mbean.server( ).getDomains( ) ) );
        LOG.info( "Exported MBean: " + mbean );
      }
    } catch ( ScriptExecutionFailedException ex ) {
      LOG.error( "Exporting MBean failed: " + ex.getMessage( ), ex );
    } catch ( IOException ex ) {
      LOG.error( "Error after export MBean: " + ex.getMessage( ), ex );
    }
  }
  
}
