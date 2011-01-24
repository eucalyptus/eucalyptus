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
package com.eucalyptus.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import com.eucalyptus.auth.Authentication;
import com.eucalyptus.auth.entities.ClusterCredentials;
import com.eucalyptus.bootstrap.Component;
import com.eucalyptus.cluster.event.NewClusterEvent;
import com.eucalyptus.config.ClusterConfiguration;
import com.eucalyptus.entities.EntityWrapper;
import com.eucalyptus.event.AbstractNamedRegistry;
import com.eucalyptus.event.ClockTick;
import com.eucalyptus.event.Hertz;
import com.eucalyptus.event.ListenerRegistry;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.msgs.RegisterClusterType;

public class Clusters extends AbstractNamedRegistry<Cluster> {
  private static Clusters singleton = getInstance( );
  private static Logger   LOG       = Logger.getLogger( Clusters.class );
  
  public static Clusters getInstance( ) {
    synchronized ( Clusters.class ) {
      if ( singleton == null ) singleton = new Clusters( );
    }
    return singleton;
  }
    
  public static Cluster start( ClusterConfiguration c ) throws EucalyptusCloudException {
    String clusterName = c.getName( );
    if ( Clusters.getInstance( ).contains( clusterName ) ) {
      return Clusters.getInstance( ).lookup( clusterName );
    } else {
      ClusterCredentials credentials = null;//ASAP: fix it.
      EntityWrapper<ClusterCredentials> credDb = Authentication.getEntityWrapper( );
      try {
        credentials = credDb.getUnique( new ClusterCredentials( c.getName( ) ) );
        credDb.rollback( );
      } catch ( EucalyptusCloudException e ) {
        LOG.error( "Failed to load credentials for cluster: " + c.getName( ) );
        credDb.rollback( );
        throw e;
      }
      Cluster newCluster = new Cluster( c, credentials );
      Clusters.getInstance( ).register( newCluster );
      newCluster.start( );
      return newCluster;
    }
  }

  public boolean hasNetworking( ) {
    return Iterables.all( Clusters.getInstance( ).listValues( ), new Predicate<Cluster>( ) {
      @Override
      public boolean apply( Cluster arg0 ) {
        return arg0.getState( ).getMode( ) == 1;
      }
    } );
  }
  
  public List<RegisterClusterType> getClusters( ) {
    List<RegisterClusterType> list = new ArrayList<RegisterClusterType>( );
    for ( Cluster c : this.listValues( ) )
      list.add( c.getWeb( ) );
    return list;
  }
  
  public List<String> getClusterAddresses( ) {
    SortedSet<String> hostOrdered = new TreeSet<String>( );
    for ( Cluster c : this.listValues( ) )
      hostOrdered.add( c.getConfiguration( ).getHostName( ) );
    return Lists.newArrayList( hostOrdered );
  }
  
  public static void stop( String name ) {
    Cluster cluster = Clusters.getInstance( ).lookup( name );
    cluster.stop( );
    Clusters.getInstance( ).deregister( name );
  }
  
}
