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
 ******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.cloud.cluster;

import com.eucalyptus.cluster.Cluster;
import com.eucalyptus.cluster.Clusters;
import com.eucalyptus.util.EucalyptusCloudException;
import com.google.common.collect.Lists;
import edu.ucsb.eucalyptus.cloud.*;
import edu.ucsb.eucalyptus.cloud.entities.*;
import edu.ucsb.eucalyptus.cloud.net.Addresses;
import edu.ucsb.eucalyptus.msgs.*;
import edu.ucsb.eucalyptus.util.Admin;
import org.apache.log4j.Logger;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.security.GeneralSecurityException;

public class ClusterEndpoint implements Startable {

  private static Logger LOG = Logger.getLogger( ClusterEndpoint.class );

  public void start() throws MuleException {
    Clusters.getInstance();
  }

  public void fire( List<RegisterClusterType> clusterChanges ) {
//TODO: handle configuration changes.
//    LOG.warn( "Processing new list of clusters: ");
//    for( RegisterClusterType c : clusterChanges ) {
//      LOG.warn( "Cluster: " + c.getName() + " host=" + c.getHost() + ":" + c.getPort() );
//    }
//    Clusters.getInstance().update( clusterChanges );
  }

//  public AddClusterResponseType fire( AddClusterType request ) throws EucalyptusCloudException {
//    if( !request.isAdministrator() ) {
//      throw new EucalyptusCloudException("Only admins can add clusters.");
//    }
//    for ( ClusterStateType c : Clusters.getInstance().getClusters() ) {
//      if( c.getName().equals( request.getName() ) ) {
//        throw new EucalyptusCloudException("Cluster already exists: " + request.getName() );
//      }
//    }
//    Clusters.getInstance().add( new ClusterStateType( request.getName(), request.getHost(), request.getPort() ) );
//    return (AddClusterResponseType) request.getReply();
//  }

  public void enqueue( EucalyptusMessage msg ) {
    LOG.error( "Intentionally dropping generic message: " + msg );
  }

  public void networkChange( Network net ) {
    try {
      Network existingNet = Networks.getInstance().lookup( net.getName() );
      ConfigureNetworkType msg = null;

      if ( net.getRules().isEmpty() ) {
        msg = Admin.makeMsg( ConfigureNetworkType.class );
        msg.setUserId( existingNet.getUserName() );
        for ( PacketFilterRule pf : existingNet.getRules() )
          msg.getRules().add( PacketFilterRule.revoke( pf ) );
        existingNet.setRules( net.getRules() );
      } else {
        existingNet.setRules( net.getRules() );
        msg = Admin.makeMsg( ConfigureNetworkType.class );
        msg.setUserId( existingNet.getUserName() );
        msg.setRules( existingNet.getRules() );
      }
      for ( NetworkToken token : existingNet.getNetworkTokens().values() )
        try {
          Cluster cluster = Clusters.getInstance().lookup( token.getCluster() );
          cluster.getMessageQueue().enqueue( new QueuedEvent<ConfigureNetworkType>( new ConfigureNetworkCallback(cluster.getConfiguration( )), msg ) );
        } catch ( NoSuchElementException e ) {}
    } catch ( NoSuchElementException e ) {
      LOG.error( "Changed network rules not applied to inactive network: " + net.getName() );
    }
  }

  public void enqueue( ClusterEnvelope msg ) {
    Clusters.getInstance().lookup( msg.getClusterName() ).getMessageQueue().enqueue( msg.getEvent() );
    RequestContext.getEventContext().setStopFurtherProcessing( true );
  }

  public void enqueue( VmAllocationInfo vmAllocInfo ) {
    VmAllocationTransaction vmTx = new VmAllocationTransaction( vmAllocInfo );
    vmTx.start();
    RequestContext.getEventContext().setStopFurtherProcessing( true );
  }

  public DescribeAvailabilityZonesResponseType DescribeAvailabilityZones( DescribeAvailabilityZonesType request ) {
    DescribeAvailabilityZonesResponseType reply = ( DescribeAvailabilityZonesResponseType ) request.getReply();
    if ( request.isAdministrator() && request.getAvailabilityZoneSet().lastIndexOf( "help" ) == 0 ) {
      reply.getAvailabilityZoneInfo().addAll( this.addHelpInfo() );
      return reply;
    }
    Collection<Cluster> clusterList = Clusters.getInstance().getEntries();
    for ( Cluster c : clusterList ) {
      reply.getAvailabilityZoneInfo().add( new ClusterInfoType( c.getConfiguration( ).getName( ), c.getConfiguration( ).getHostName( ) ) );
//TODO: handle cluster reachability
//      if( !c.isReachable() ) {
//        continue;
//      }
      List<String> args = request.getAvailabilityZoneSet();
      NavigableSet<String> tagList = new ConcurrentSkipListSet<String>( request.getAvailabilityZoneSet() );
      if ( tagList.size() == 1 ) tagList = c.getNodeTags();
      else
        tagList.retainAll( c.getNodeTags() );
      if ( tagList.isEmpty() ) return reply;


      if ( request.isAdministrator() && args.lastIndexOf( "verbose" ) == 0 )
        reply.getAvailabilityZoneInfo().addAll( this.addSystemInfo( c ) );
      else if ( request.isAdministrator() && args.lastIndexOf( "certs" ) == 0 )
        for ( String tag : tagList )
          reply.getAvailabilityZoneInfo().addAll( this.addCertInfo( tag, c ) );
      else if ( request.isAdministrator() && args.lastIndexOf( "logs" ) == 0 )
        for ( String tag : tagList )
          reply.getAvailabilityZoneInfo().addAll( this.addLogInfo( tag, c ) );
      else if ( request.isAdministrator() && args.lastIndexOf( "coredump" ) == 0 )
        reply.getAvailabilityZoneInfo().addAll( this.dumpState() );

    }
    return reply;
  }

  private static String INFO_FSTRING = "|- %s";
  private static String HEADER_STRING = "free / max   cpu   ram  disk";
  private static String STATE_FSTRING = "%04d / %04d  %2d   %4d  %4d";

  private static ClusterInfoType t( String left, String right ) { return new ClusterInfoType( left, right ); }

  private static ClusterInfoType s( String left, String right ) { return new ClusterInfoType( String.format( INFO_FSTRING, left ), right );}

  private List<ClusterInfoType> dumpState() {
    List<ClusterInfoType> retList = Lists.newArrayList();
    for ( Address addr : Addresses.getInstance().listValues() ) {
      String val = addr.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    for ( Address addr : Addresses.getInstance().listDisabledValues() ) {
      String val = addr.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    for ( VmInstance vm : VmInstances.getInstance().listValues() ) {
      String val = vm.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    for ( VmInstance vm : VmInstances.getInstance().listDisabledValues() ) {
      String val = vm.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    for ( Cluster cluster : Clusters.getInstance().listValues() ) {
      String val = cluster.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    for ( Network network : Networks.getInstance().listValues() ) {
      String val = network.toString();
      retList.add( new ClusterInfoType( val, "" ) );
      LOG.info( val );
    }
    return retList;
  }

  private Collection<ClusterInfoType> addHelpInfo() {
    List<ClusterInfoType> helpInfo = new ArrayList<ClusterInfoType>();
    helpInfo.add( t( "sub-command", "effect & arguments" ) );
    helpInfo.add( s( "logs     [SERVICE_TAGS...]", "get log files from the system." ) );
    helpInfo.add( s( "certs    [SERVICE_TAGS...]", "get log files from the system." ) );
    helpInfo.add( s( "verbose", "get log files from the system." ) );
    return helpInfo;
  }

  private List<ClusterInfoType> addSystemInfo( final Cluster cluster ) {
    List<ClusterInfoType> info = new ArrayList<ClusterInfoType>();
    try {
      info.add( new ClusterInfoType( String.format( INFO_FSTRING, "vm types" ), HEADER_STRING ) );
      for ( VmType v : VmTypes.list() ) {
        VmTypeAvailability va = cluster.getNodeState().getAvailability( v.getName() );
        info.add( s( v.getName(), String.format( STATE_FSTRING, va.getAvailable(), va.getMax(), v.getCpu(), v.getMemory(), v.getDisk() ) ) );
      }
      for ( String s : cluster.getNodeTags() ) {
        NodeInfo node = cluster.getNode( s );
        String certs = "certs[cc=%s,nc=%s] @ %s";
        String ccAlias = "";
        String ncAlias = "";
        //TODO: IMPORTANT fix me
//        try {
//          ccAlias = ServiceKeyStore.getInstance().getCertificateAlias( node.getCerts().getCcCert() );
//          if ( ccAlias == null ) ccAlias = "FALSE";
//          ncAlias = ServiceKeyStore.getInstance().getCertificateAlias( node.getCerts().getNcCert() );
//          if ( ncAlias == null ) ncAlias = "FALSE";
//        } catch ( GeneralSecurityException e ) {}
        info.add( s( node.getName(), String.format( certs, ccAlias.startsWith( "cc-" ), ncAlias.startsWith( "nc-" ), node.getLastSeen() ) ) );
      }
    }
    catch ( Exception e ) {
      LOG.error( e, e );
    }

    return info;
  }

  private List<ClusterInfoType> addLogInfo( final String serviceTag, final Cluster cluster ) {
    List<ClusterInfoType> info = new ArrayList<ClusterInfoType>();
    NodeInfo node = cluster.getNode( serviceTag );
    NodeLogInfo logInfo = node.getLogs();
    info.add( t( node.getName(), "last-seen=" + node.getLastSeen() ) );
    if ( !logInfo.getNcLog().isEmpty() )
      info.add( s( "nc.log\n", logInfo.getNcLog() ) );
    if ( !logInfo.getNcLog().isEmpty() )
      info.add( s( "cc.log\n", logInfo.getCcLog() ) );
    info.add( t( node.getName(), "last-seen=" + node.getLastSeen() ) );
    info.add( s( "axis2.log\n", logInfo.getAxis2Log() ) );
    info.add( t( node.getName(), "last-seen=" + node.getLastSeen() ) );
    info.add( s( "httpd.log\n", logInfo.getHttpdLog() ) );
    return info;
  }

  private Collection<ClusterInfoType> addCertInfo( final String serviceTag, final Cluster c ) {
    List<ClusterInfoType> info = new ArrayList<ClusterInfoType>();
    NodeInfo node = c.getNode( serviceTag );
    info.add( t( node.getName(), "last-seen=" + node.getLastSeen() ) );
    NodeCertInfo certInfo = node.getCerts();
    info.add( s( "CC cert\n", certInfo.getCcCert() ) );
    info.add( s( "NC cert\n", certInfo.getCcCert() ) );
    return info;
  }

}
//:: keytool -list -rfc -storetype bks -storepass eucalyptus -provider org.bouncycastle.jce.provider.BouncyCastleProvider -keystore conf/eucalyptus.bks
