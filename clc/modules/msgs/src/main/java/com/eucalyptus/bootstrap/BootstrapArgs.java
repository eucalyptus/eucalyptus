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

package com.eucalyptus.bootstrap;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.log4j.Logger;
import com.eucalyptus.system.SubDirectory;
import com.eucalyptus.util.Internets;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

public class BootstrapArgs {
  private static Logger             LOG            = Logger.getLogger( BootstrapArgs.class );
  private static final List<String> bindAddrs      = Lists.newArrayList( );
  private static final List<String> bootstrapHosts = Lists.newArrayList( );
  private static boolean            initSystem     = false;
  
  enum BindAddressValidator implements Predicate<String> {
    INSTANCE;
    @Override
    public boolean apply( String arg0 ) {
      try {
        InetAddress.getByName( arg0 );
        return true;
      } catch ( UnknownHostException ex ) {
        LOG.error( ex, ex );
        return false;
      }
    }
    
  }
  
  enum BootstrapHostValidator implements Predicate<String> {
    INSTANCE;
    @Override
    public boolean apply( String arg0 ) {
      try {
        InetAddress.getByName( arg0 );
        return true;
      } catch ( UnknownHostException ex ) {
        LOG.error( ex, ex );
        return false;
      }
    }
    
  }
  
  static void init( ) {
    try {
      InetAddress.getByName( "eucalyptus.com" ).isReachable( NetworkInterface.getByInetAddress(Internets.localHostInetAddress( )), 64, 10000 );//GRZE:attempted hack to allocate raw socket
    } catch ( Exception ex2 ) {
      LOG.error( ex2 , ex2 );
    }
    bindAddrs.addAll( BootstrapArgs.parseMultipleArgs( "euca.bind.addr", BindAddressValidator.INSTANCE ) );
    bootstrapHosts.addAll( BootstrapArgs.parseMultipleArgs( "euca.bootstrap.host", BindAddressValidator.INSTANCE ) );
    initSystem = System.getProperty( "euca.initialize" ) != null;
  }
  
  public static boolean isInitializeSystem( ) {
    return initSystem;
  }
  
  public static List<String> parseBootstrapHosts( ) {
    return bootstrapHosts;
  }
  
  public static List<String> bindAddresses( ) {
    return bindAddrs;
  }
  
  private static List<String> parseMultipleArgs( String baseString, Predicate<String> argValidator ) {
    List<String> retList = Lists.newArrayList( );
    String formatString = baseString + ".%d";
    String next = String.format( formatString, 0 );
    for ( int i = 0; i < 255; next = String.format( formatString, i++ ) ) {
      String nextVal = System.getProperty( next );
      if ( nextVal != null ) {
        if ( argValidator.apply( nextVal ) ) {
          retList.add( System.getProperty( next ) );
        } else {
          Error err = new ArgumentValidationError( "Argument validation failed for " + next + " on value: " + nextVal );
          LOG.error( err, err );
        }
      }
    }
    return retList;
    
  }
  
  public static Boolean isCloudController( ) {
    return SubDirectory.DB.hasChild( "data", "ibdata1" ) && !Boolean.TRUE.valueOf( System.getProperty( "euca.force.remote.bootstrap" ) );
  }

  public static String debugTopology( ) {
    return System.getProperty( "euca.noha.cloud" );
  }
  
}
