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

import groovy.jmx.builder.JmxBuilder;
import groovy.util.GroovyMBean;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.BootstrapException;
import com.eucalyptus.records.Logs;
import com.eucalyptus.scripting.Groovyness;
import com.eucalyptus.scripting.ScriptExecutionFailedException;
import com.eucalyptus.system.SubDirectory;
import com.google.common.io.Files;

public class Mbeans {
  private static final Map<String, String> EMPTY    = new HashMap<String, String>( );
  private static Logger                    LOG      = Logger.getLogger( Mbeans.class );
  private static final int                 JMX_PORT = 1099;                                                                        //TODO:GRZE: configurable
//private static final int                 JMX_PORT = 8772;
  private static final String              JMX_HOST = "localhost";                                                                 //TODO:GRZE: configurable
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
  static {
    Mbeans.init( );
  }
  
  public static void init( ) {////TODO:GRZE: make it a bootstrapper
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
  
  public static <T> T lookup( final String domain, final Map props, Class<T> type  ) throws NoSuchElementException {
    ObjectName objectName;
    Hashtable<String, String> attributes = new Hashtable<String, String>( props );
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer( );
      objectName = ObjectName.getInstance( domain, attributes );
      T mbeanProxy = JMX.newMBeanProxy( server, objectName, type );
      return mbeanProxy;
    } catch ( MalformedObjectNameException ex ) {
      Logs.extreme( ).error( ex , ex );
      throw new NoSuchElementException( "Failed to lookup: " + type.getCanonicalName( ) + " named: " + domain + "=" + props.toString( ) );
    } catch ( NullPointerException ex ) {
      Logs.extreme( ).error( ex , ex );
      throw new NoSuchElementException( "Failed to lookup: " + type.getCanonicalName( ) + " named: " + domain + "=" + props.toString( ) );
    }
  }

  public static void register( final Object obj ) {
    Class targetType = obj.getClass( );
    if( targetType.isAnonymousClass( ) ) {
      targetType = ( targetType.getSuperclass( ) != null ? targetType.getSuperclass( ) : targetType.getInterfaces( )[0]);
    }
    String exportString = "jmx.export{ bean( " +
    " target: obj, " +
    " name: obj.class.package.name+\":type=${obj.class.simpleName}\"," +
    " desc: \"${obj.toString()}\"" +
    " ) }";
    for( Class c : Classes.ancestors( targetType ) ) {
      File jmxConfig = SubDirectory.MANAGEMENT.getChildFile( c.getCanonicalName( ) );
      if(  jmxConfig.exists( ) ) {
        LOG.trace( "Trying to read jmx config file: " + jmxConfig.getAbsolutePath( ) );
        try {
          exportString = Files.toString( jmxConfig, Charset.defaultCharset( ) );
          LOG.trace( "Succeeded reading jmx config file: " + jmxConfig.getAbsolutePath( ) );
          break;
        } catch ( IOException ex ) {
          LOG.error( ex , ex );
        }
      }
    }
    //TODO:GRZE:load class specific config here
    try {
      LOG.trace( "Exporting MBean: " + obj );
      LOG.trace( "Exporting MBean: " + exportString );
      List<GroovyMBean> mbeans = ( List<GroovyMBean> ) Groovyness.eval( exportString, new HashMap( ) {
        {
          put( "jmx", jmxBuilder );
          put( "obj", obj );
        }
      } );
      for ( GroovyMBean mbean : mbeans ) {
        LOG.trace( "MBean server: default=" + mbean.server( ).getDefaultDomain( ) + " all=" + Arrays.asList( mbean.server( ).getDomains( ) ) );
        LOG.trace( "Exported MBean: " + mbean );
      }
    } catch ( ScriptExecutionFailedException ex ) {
      LOG.error( "Exporting MBean failed: " + ex.getMessage( ), ex );
    } catch ( IOException ex ) {
      LOG.error( "Error after export MBean: " + ex.getMessage( ), ex );
    }
  }
}
